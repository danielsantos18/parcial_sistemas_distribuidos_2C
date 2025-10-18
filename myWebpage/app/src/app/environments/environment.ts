export const environment = {
  production: false,

  // Usuarios
  apiUsers: 'https://sdamtp8yo3.execute-api.us-east-2.amazonaws.com/users',
  apiREgister: 'https://sdamtp8yo3.execute-api.us-east-2.amazonaws.com/users/register',
  apiLogin: 'https://akhmew3pv1.execute-api.us-east-2.amazonaws.com/users/login',
  apiProfile: 'https://v7hjhcn0ej.execute-api.us-east-2.amazonaws.com/users/profile',
  apiUpdateProfile: 'https://dppejwy8a1.execute-api.us-east-2.amazonaws.com/users/profile',
  apiAvatar: 'https://liuisg70kg.execute-api.us-east-2.amazonaws.com/users/profile',

  // Tarjetas
  apiCardRequest: 'https://5ijhgybl79.execute-api.us-east-2.amazonaws.com/prod/cards/request',
  apiCardActivate: 'https://9kmh52ohqg.execute-api.us-east-2.amazonaws.com/prod/card/activate',
  apiCardGet: 'https://jo9pd89ssf.execute-api.us-east-2.amazonaws.com/card/profile',
  apiCardPaid: 'https://u7y5b7s9h5.execute-api.us-east-2.amazonaws.com/card/paid',
  
  // Transacciones
  apiTransactionPurchase: 'https://v5h6q897r2.execute-api.us-east-2.amazonaws.com/transaction/purchase',
  apiTransactionSave: 'https://a3iikkvakf.execute-api.us-east-2.amazonaws.com/transaction/save',

  // Catálogo y pagos
  apiCatalog: 'https://q6mfnxm2i4.execute-api.us-east-2.amazonaws.com/dev/catalog',
  apiCatalogUpload: 'https://h37d81gbf9.execute-api.us-east-2.amazonaws.com/catalog/update',
  apiPayments: 'https://cgvrj6g5gh.execute-api.us-east-2.amazonaws.com/catalog/payment',
};
