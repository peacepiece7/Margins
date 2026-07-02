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
    expect(translationCatalog.en.username).toBe('Username');
    expect(translationCatalog.en.password).toBe('Password');
    expect(translationCatalog.en.logout).toBe('Logout');
  });

  it('keeps Korean login controls in Korean', () => {
    expect(translationCatalog.ko.username).toBe('사용자 이름');
    expect(translationCatalog.ko.password).toBe('비밀번호');
    expect(translationCatalog.ko.logout).toBe('로그아웃');
  });
});
