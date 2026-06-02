import type { AxiosError } from 'axios'
import axios from 'axios'
import { extractErrorCode } from '~/utils/errorCodeMap'
import useAuth from './useAuth'

type RequestExecutor<T> = () => Promise<{ data: T }>

function serializeWithPrecision(data: any): string {
  return JSON.stringify(data, (key, value) => {
    if (typeof value === 'number' && !Number.isNaN(value) && Number.isFinite(value)) {
      const isMonetaryKey = /amount|value|digit|price|cost/i.test(key)
      const hasDecimals = value % 1 !== 0
      if (isMonetaryKey || hasDecimals) {
        return value.toFixed(2)
      }
    }
    return value
  })
}

export default function useQuery() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl
  const { logout, tryRefresh } = useAuth()

  async function executeWithAuthRetry<T>(request: RequestExecutor<T>): Promise<T | undefined> {
    try {
      const response = await request()
      return response.data
    } catch (error: any) {
      return handleError(error, request)
    }
  }

  async function get(url: string, params: any | undefined = undefined) {
    return executeWithAuthRetry(() => axios.get(`${host}${url}`, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
      },
      params,
    }))
  }

  async function deleteQuery(url: string, body: any | undefined) {
    return executeWithAuthRetry(() => axios.delete(`${host}${url}`, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
      },
      data: body,
      transformRequest: [serializeWithPrecision],
    }))
  }

  async function post(url: string, body: any | undefined) {
    return executeWithAuthRetry(() => axios.post(`${host}${url}`, body, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
      },
      transformRequest: [serializeWithPrecision],
    }))
  }

  async function patch(url: string, body: any | undefined) {
    return executeWithAuthRetry(() => axios.patch(`${host}${url}`, body, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
      },
      transformRequest: [serializeWithPrecision],
    }))
  }

  async function put(url: string, body: any | undefined) {
    return executeWithAuthRetry(() => axios.put(`${host}${url}`, body, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
      },
      transformRequest: [serializeWithPrecision],
    }))
  }

  async function handleError<T>(error: Error, request?: RequestExecutor<T>): Promise<T | undefined> {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<any, any>
      const status = axiosError.response?.status
      const domainCode = extractErrorCode(axiosError.response?.data)

      if (domainCode === 1 && request) {
        const refreshed = await tryRefresh({ redirectOnFailure: false })
        if (!refreshed) {
          await logout()
          return
        }

        try {
          const response = await request()
          return response.data
        } catch (retryError: any) {
          return handleError(retryError)
        }
      }

      if (status === 401 || status === 403) {
        await logout()
        return
      }
    }

    throw error
  }

  return { get, post, deleteQuery, patch, put }
}
