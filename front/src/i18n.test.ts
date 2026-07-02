import { describe, expect, it } from 'vitest';
import { translationCatalog } from './i18n';

describe('translation catalog', () => {
  it('keeps English and Korean translation keys aligned', () => {
    expect(Object.keys(translationCatalog.ko).sort()).toEqual(Object.keys(translationCatalog.en).sort());
  });

  it('keeps all translation values populated', () => {
    Object.values(translationCatalog).forEach((messages) => {
      Object.entries(messages).forEach(([key, value]) => {
        expect(value, key).toBeTruthy();
      });
    });
  });

  it('keeps English as the default product language', () => {
    expect(translationCatalog.en.loginSubtitle).toContain('private reading archive');
    expect(translationCatalog.en.deleteConfirm).toBe('Delete this item?');
    expect(translationCatalog.en.pageBookSearch).toBe('Discover');
  });
});
