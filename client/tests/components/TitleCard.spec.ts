import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TitleCard from '../../components/TitleCard.vue'

describe('components/TitleCard', () => {
  it('renders title and description', () => {
    const wrapper = mount(TitleCard, {
      props: {
        title: 'Dashboard',
        description: 'Open your dashboard',
      },
    })

    expect(wrapper.text()).toContain('Dashboard')
    expect(wrapper.text()).toContain('Open your dashboard')
  })

  it('calls onClick when card is clicked', async () => {
    const onClick = vi.fn()
    const wrapper = mount(TitleCard, {
      props: {
        title: 'Reports',
        description: 'Open reports',
        onClick,
      },
    })

    await wrapper.trigger('click')

    expect(onClick).toHaveBeenCalledTimes(1)
  })
})
