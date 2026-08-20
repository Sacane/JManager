import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readonly, ref } from 'vue'
import FeatureGate from '../../components/FeatureGate.vue'
import { FEATURE_KEYS } from '../../constants/featureKeys'

const isEnabledMock = vi.fn(() => false)

function makeFlags(enabled: boolean) {
  isEnabledMock.mockReturnValue(enabled)
  vi.stubGlobal('useFeatureFlags', vi.fn(() => ({
    flags: readonly(ref([])),
    isFetching: ref(false),
    isToggling: ref(false),
    isEnabled: isEnabledMock,
    fetchFlags: vi.fn(),
    toggleFlag: vi.fn(),
  })))
}

describe('components/FeatureGate', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the default slot when the flag is enabled', () => {
    makeFlags(true)
    const wrapper = mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: { default: '<span data-testid="content">visible</span>' },
    })
    expect(wrapper.find('[data-testid="content"]').exists()).toBe(true)
  })

  it('does not render the default slot when the flag is disabled', () => {
    makeFlags(false)
    const wrapper = mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: { default: '<span data-testid="content">hidden</span>' },
    })
    expect(wrapper.find('[data-testid="content"]').exists()).toBe(false)
  })

  it('renders nothing visible when the flag is disabled and no fallback is provided', () => {
    makeFlags(false)
    const wrapper = mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: { default: '<span data-testid="content">hidden</span>' },
    })
    expect(wrapper.find('[data-testid="content"]').exists()).toBe(false)
    expect(wrapper.text()).toBe('')
  })

  it('renders the fallback slot when the flag is disabled', () => {
    makeFlags(false)
    const wrapper = mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: {
        default: '<span data-testid="content">hidden</span>',
        fallback: '<span data-testid="fallback">fallback</span>',
      },
    })
    expect(wrapper.find('[data-testid="fallback"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="content"]').exists()).toBe(false)
  })

  it('does not render the fallback when the flag is enabled', () => {
    makeFlags(true)
    const wrapper = mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: {
        default: '<span data-testid="content">visible</span>',
        fallback: '<span data-testid="fallback">fallback</span>',
      },
    })
    expect(wrapper.find('[data-testid="content"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="fallback"]').exists()).toBe(false)
  })

  it('calls isEnabled with the correct feature key', () => {
    makeFlags(true)
    mount(FeatureGate, {
      props: { feature: FEATURE_KEYS.USER_REGISTRATION },
      slots: { default: '<span />' },
    })
    expect(isEnabledMock).toHaveBeenCalledWith(FEATURE_KEYS.USER_REGISTRATION)
  })
})
