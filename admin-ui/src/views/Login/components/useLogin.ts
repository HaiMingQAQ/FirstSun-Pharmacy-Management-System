import { Ref } from 'vue'

export enum LoginStateEnum {
  LOGIN,
  REGISTER,
  RESET_PASSWORD,
  MOBILE,
  QR_CODE,
  SSO
}

export enum LoginModeEnum {
  PHARMACY = 'pharmacy',
  PLATFORM = 'platform'
}

const currentState = ref(LoginStateEnum.LOGIN)
const currentLoginMode = ref(LoginModeEnum.PHARMACY)

export function useLoginState() {
  function setLoginState(state: LoginStateEnum) {
    currentState.value = state
  }
  const getLoginState = computed(() => currentState.value)
  const getLoginMode = computed(() => currentLoginMode.value)

  function setLoginMode(mode: LoginModeEnum) {
    currentLoginMode.value = mode
  }

  function handleBackLogin() {
    setLoginState(LoginStateEnum.LOGIN)
  }

  return {
    setLoginState,
    getLoginState,
    setLoginMode,
    getLoginMode,
    handleBackLogin
  }
}

export function useFormValid<T extends Object = any>(formRef: Ref<any>) {
  async function validForm() {
    const form = unref(formRef)
    if (!form) return
    const data = await form.validate()
    return data as T
  }

  return {
    validForm
  }
}
