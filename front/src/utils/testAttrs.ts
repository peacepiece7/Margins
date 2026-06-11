export function testAttr(name: string): Record<string, string> {
  if (import.meta.env.PROD) {
    return {};
  }
  return { 'data-testid': name };
}
