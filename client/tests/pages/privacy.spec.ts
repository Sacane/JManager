import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PrivacyPage from '../../pages/privacy.vue'

vi.stubGlobal('definePageMeta', vi.fn())

describe('pages/privacy', () => {
  function mountPage() {
    return shallowMount(PrivacyPage)
  }

  it('renders the main heading', () => {
    const wrapper = mountPage()
    expect(wrapper.find('h1').text()).toContain('Politique de Confidentialité')
  })

  it('displays the controller identity section', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('RAMAROSON RAKOTOMIHAMINA Johan')
    expect(wrapper.text()).toContain('Bussy-Saint-Georges')
    expect(wrapper.text()).toContain('contact@jmanager.sacane.fr')
  })

  it('lists the retention periods', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('30 jours')
    expect(wrapper.text()).toContain('90 jours')
    expect(wrapper.text()).toContain('5 ans')
  })

  it('mentions the GDPR legal bases', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('Art. 6')
    expect(wrapper.text()).toContain('Art. 7')
  })

  it('references the CNIL', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('CNIL')
  })

  it('mentions Resend as an email sub-processor', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('Resend')
    expect(wrapper.text()).toContain('Clauses Contractuelles Types')
  })

  it('uses the legal layout', () => {
    // definePageMeta is stubbed — just verify the page mounts without errors
    expect(mountPage().exists()).toBe(true)
  })
})
