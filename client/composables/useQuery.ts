import type { AxiosError } from 'axios'
import axios from 'axios'
import useAuth from './useAuth'

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
  const { logout } = useAuth()

  async function get(url: string, params: any | undefined = undefined) {
    try {
      const response = await axios.get(`${host}${url}`, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
        },
        params,
      })
      return response.data
    } catch (error: any) {
      console.warn('handle error')
      handleError(error)
    }
  }
  async function deleteQuery(url: string, body: any | undefined) {
    try {
      const response = await axios.delete(`${host}${url}`, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
        },
        data: body,
        transformRequest: [serializeWithPrecision],
      })
      return response.data
    } catch (error: any) {
      handleError(error)
    }
  }

  async function post(url: string, body: any | undefined) {
    try {
      const response = await axios.post(`${host}${url}`, body, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
        },
        transformRequest: [serializeWithPrecision],
      })
      return response.data
    } catch (error: any) {
      handleError(error)
    }
  }
  async function patch(url: string, body: any | undefined) {
    try {
      const response = await axios.patch(`${host}${url}`, body, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
        },
        transformRequest: [serializeWithPrecision],
      })
      return response.data
    } catch (error: any) {
      handleError(error)
    }
  }

  async function put(url: string, body: any | undefined) {
    try {
      const response = await axios.put(`${host}${url}`, body, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
        },
        transformRequest: [serializeWithPrecision],
      })
      return response.data
    } catch (error: any) {
      handleError(error)
    }
  }

  function handleError(error: Error) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<any, any>
      const status = axiosError.response?.data.status
      // const message = axiosError.response?.data.message
      if (status === 401 || status === 403) {
        // toast.error(message)
        localStorage.removeItem('user')
        logout().then(() => {
          navigateTo('/login')
        })
        return
      }
      // toast.error(message)
    }
    throw error
  }

  return { get, post, deleteQuery, patch, put }
}
