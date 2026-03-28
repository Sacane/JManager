export const LOADING_SCOPES = {
  admin: {
    fetchUsers: 'admin.fetchUsers',
    createUser: 'admin.createUser',
  },
  dashboard: {
    initial: 'dashboard.initial',
  },
  accountIndex: {
    load: 'account.index.load',
    create: 'account.index.create',
    delete: 'account.index.delete',
  },
  accountDetails: {
    load: 'account.loadBookletData',
    createTransaction: 'account.bookTransaction',
    fetchTransaction: 'account.fetchTransaction',
    editTransaction: 'account.editTransaction',
    deleteTransaction: 'account.deleteTransaction',
    confirmPreview: 'account.confirmPreview',
    exportCsv: 'account.exportCsv',
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
