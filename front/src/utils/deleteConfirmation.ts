export const deleteConfirmationMessage = '삭제하시겠습니까?';

export function confirmDelete() {
  if (typeof window === 'undefined') {
    return true;
  }

  return window.confirm(deleteConfirmationMessage);
}
