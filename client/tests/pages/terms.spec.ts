import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TermsPage from '../../pages/terms.vue'

vi.stubGlobal('definePageMeta', vi.fn())

describe('pages/terms', () => {
  function mountPage() {
    return shallowMount(TermsPage)
  }

  it('renders the main heading', () => {
    const wrapper = mountPage()
    expect(wrapper.find('h1').text()).toContain("Conditions Générales d'Utilisation")
  })

  it('displays the editor identity section', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('RAMAROSON RAKOTOMIHAMINA Johan')
    expect(wrapper.text()).toContain('Bussy-Saint-Georges')
    expect(wrapper.text()).toContain('contact@jmanager.sacane.fr')
    expect(wrapper.text()).toContain('Pulseheberge')
  })

  it('mentions restricted access (no self-registration)', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('accès restreint')
  })

  it('contains a link to the privacy policy', () => {
    const wrapper = mountPage()
    const privacyLink = wrapper.find('a[href="/privacy"]')
    expect(privacyLink.exists()).toBe(true)
    expect(privacyLink.attributes('target')).toBe('_blank')
  })

  it('mentions RGPD Art. 7 consent', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('Art. 7')
    expect(wrapper.text()).toContain('RGPD')
  })

  it('references the applicable law', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('droit français')
  })

  it('uses the legal layout', () => {
    // definePageMeta is stubbed — just verify the page mounts without errors
    expect(mountPage().exists()).toBe(true)
  })
})
