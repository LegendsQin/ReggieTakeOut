function loginApi(data) {
    return $axios({
      'url': '/user/login',
      'method': 'post',
      data
    })
  }

function loginoutApi() {
  return $axios({
    'url': '/user/loginout',
    'method': 'post',
  })
}

function getSmsCodeApi(data) {
    return $axios({
        'url': '/user/sendMsg', // 后端接口地址（需和后端约定）
        //如果用 GET 请求，手机号会拼在 URL 里：GET /user/getSmsCode?phone=13800138000
        'method': 'post',   //安全性：避免手机号暴露在 URL 中（最关键）
        data // 参数：主要传手机号
    })
}

  