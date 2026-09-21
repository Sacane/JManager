import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import usePasswordPolicy from '../../composables/usePasswordPolicy'

vi.mock('axios')

const PUBLISHED = { minLength: 12, maxLength: 100, rules: ['too_short', 'too_long', 'equals_email'] }

describe('usePasswordPolicy', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset()
    // One shared state per test, as useState gives one per key in the app.
    const shared = ref(null)
    vi.stubGlobal('useState', vi.fn(() => shared))
  })

  it('loads the policy the server publishes', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: PUBLISHED })
    const { policy, load } = usePasswordPolicy()

    await load()

    expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/api/password-policy')
    expect(policy.value).toEqual(PUBLISHED)
  })

  it('asks the server once, however many fields need the policy', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: PUBLISHED })

    await Promise.all([usePasswordPolicy().load(), usePasswordPolicy().load()])
    await usePasswordPolicy().load()

    expect(axios.get).toHaveBeenCalledTimes(1)
  })

  // The server still enforces the policy; without it the screens simply show no checklist.
  it('stays empty when the policy cannot be loaded, and tries again next time', async () => {
    vi.mocked(axios.get).mockRejectedValueOnce(new Error('network down')).mockResolvedValue({ data: PUBLISHED })
    const { policy, load } = usePasswordPolicy()

    await load()
    expect(policy.value).toBeNull()

    await load()
    expect(policy.value).toEqual(PUBLISHED)
  })

  it('ignores an answer that is not a policy', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: '<html>maintenance</html>' })
    const { policy, load } = usePasswordPolicy()

    await load()

    expect(policy.value).toBeNull()
  })
})
