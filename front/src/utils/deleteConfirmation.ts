export const deleteConfirmationMessage = 'Delete this item?';

export function confirmDelete(message = deleteConfirmationMessage) {
  if (typeof window === 'undefined') {
    return true;
  }

  return window.confirm(message);
}
