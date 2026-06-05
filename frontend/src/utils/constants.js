export const LEAVE_TYPES = ['年假', '事假', '病假', '婚假', '产假', '调休']

export const STATUS_MAP = {
  PENDING: { label: '审批中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  WITHDRAWN: { label: '已撤回', type: 'info' }
}

export const ACTION_MAP = {
  APPROVE: { label: '通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
  WITHDRAW: { label: '撤回', type: 'info' },
  TRANSFER: { label: '转派', type: 'warning' }
}

export const APPROVER_TYPES = [
  { value: 'DIRECT_LEADER', label: '直属领导' },
  { value: 'DEPARTMENT_HEAD', label: '部门总监' },
  { value: 'SPECIFIC_USER', label: '指定用户' }
]

export const SIGN_TYPES = [
  { value: 'SINGLE', label: '单人审批' },
  { value: 'COUNTER_SIGN', label: '会签（全部同意）' },
  { value: 'OR_SIGN', label: '或签（任一同意）' }
]

export const TIMEOUT_ACTIONS = [
  { value: 'ESCALATE', label: '转派给指定人' },
  { value: 'AUTO_APPROVE', label: '自动通过' },
  { value: 'AUTO_REJECT', label: '自动驳回' }
]

export const STEP_LABELS = ['直属领导审批', '部门总监审批', '完成']
