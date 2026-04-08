export const LOADING_SCOPES = {
  admin: {
    fetchUsers: 'admin.fetchUsers',
    createUser: 'admin.createUser',
  },
  dashboard: {
    initial: 'dashboard.initial',
  },
  bookletIndex: {
    load: 'booklet.index.load',
    create: 'booklet.index.create',
    delete: 'booklet.index.delete',
  },
  bookletDetails: {
    load: 'booklet.loadBookletData',
    createTransaction: 'booklet.bookTransaction',
    fetchTransaction: 'booklet.fetchTransaction',
    editTransaction: 'booklet.editTransaction',
    deleteTransaction: 'booklet.deleteTransaction',
    confirmPreview: 'booklet.confirmPreview',
    exportCsv: 'booklet.exportCsv',
    regenerate: 'booklet.regenerate',
  },
  tag: {
    load: 'tag.load',
    add: 'tag.add',
    edit: 'tag.edit',
    delete: 'tag.delete',
  },
  settings: {
    load: 'settings.load',
    save: 'settings.save',
  },
} as const
