import { useSessionFlowStore } from '../store/sessionFlowStore';

export function useSessionFlow() {
  return useSessionFlowStore();
}
