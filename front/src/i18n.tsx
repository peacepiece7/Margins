import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from 'react';

export type Locale = 'en' | 'ko';

const LOCALE_STORAGE_KEY = 'margins.locale';

const koreanTranslations: Record<string, string> = {
  'Reading record workspace': '독서 기록 작업공간',
  'Reading session workbench': '독서 세션 작업대',
  'Single-user mode': '단일 사용자 모드',
  'Session #{{id}}': '세션 #{{id}}',
  Logout: '로그아웃',
  Username: '사용자 이름',
  Password: '비밀번호',
  Login: '로그인',
  'Login failed': '로그인에 실패했습니다',
  English: '영어',
  Korean: '한국어',
  Language: '언어',
  'Search a book': '책 검색',
  Search: '검색',
  Retry: '다시 시도',
  'Library dashboard': '라이브러리 대시보드',
  '{{count}} sessions': '{{count}}개 세션',
  Completed: '완료',
  Complete: '완료하기',
  Active: '진행 중',
  Quotes: '인용',
  Answers: '답변',
  Personas: '페르소나',
  Books: '책',
  'Avg progress': '평균 진행률',
  'Reading memory': '독서 메모리',
  '{{count}} matches': '{{count}}개 결과',
  'Search notes, debate, quotes': '노트, 토론, 인용 검색',
  Find: '찾기',
  'Saved books': '저장한 책',
  '{{visible}}/{{total}} available': '{{visible}}/{{total}}개 사용 가능',
  'Filter saved books': '저장한 책 필터',
  'Unknown author': '알 수 없는 저자',
  Start: '시작',
  'No saved books match the current filter.': '현재 필터와 일치하는 저장된 책이 없습니다.',
  'Reading sessions': '독서 세션',
  '{{visible}}/{{total}} saved': '{{visible}}/{{total}}개 저장됨',
  'Filter sessions': '세션 필터',
  All: '전체',
  Pinned: '고정됨',
  'No reflection window': '회고 창 없음',
  '{{answered}}/{{total}} answered': '{{answered}}/{{total}}개 답변됨',
  'Generate questions': '질문 생성',
  Generate: '생성',
  'Filter questions': '질문 필터',
  'Reflection questions': '회고 질문',
  'All questions': '전체 질문',
  Answered: '답변됨',
  Open: '열림',
  'Select prompt': '프롬프트 선택',
  Delete: '삭제',
  'No questions match this filter.': '이 필터와 일치하는 질문이 없습니다.',
  'Current book': '현재 책',
  'No book selected': '선택된 책 없음',
  'Edit title': '제목 수정',
  'Add tag': '태그 추가',
  'Active window': '활성 창',
  'Edit window': '창 수정',
  'Archive window': '창 보관',
  'New reflection window': '새 회고 창',
  'Session areas': '세션 영역',
  Unanswered: '미답변',
  'Add your own question': '직접 질문 추가',
  Add: '추가',
  Questions: '질문',
  Progress: '진행률',
  Messages: '메시지',
  Review: '리뷰',
  Debate: '토론',
  Windows: '창',
  'Not set': '미설정',
  'Next actions': '다음 작업',
  'Set reading progress': '독서 진행률 설정',
  'Add a goal and page range so this session can track momentum.': '세션 흐름을 추적할 수 있도록 목표와 페이지 범위를 추가하세요.',
  'Generate reflection questions': '회고 질문 생성',
  'Create prompts for the current reflection window before writing answers.': '답변을 쓰기 전에 현재 회고 창의 프롬프트를 만드세요.',
  'Answer an open question': '열린 질문에 답변',
  'Save a quote': '인용 저장',
  'Capture a passage or note so the review has evidence.': '리뷰에 근거가 남도록 구절이나 메모를 저장하세요.',
  'Ask a persona': '페르소나에게 묻기',
  'Send one interpretation to the debate window for another perspective.': '다른 관점을 얻기 위해 해석 하나를 토론 창으로 보내세요.',
  'Complete this session': '이 세션 완료',
  'Write a closeout summary now that the reading goal is fully progressed.': '독서 목표가 완료되었으니 마무리 요약을 작성하세요.',
  'Review readiness': '리뷰 준비도',
  '{{ready}}/{{total}} ready': '{{ready}}/{{total}}개 준비됨',
  'Session brief': '세션 요약',
  'Active session brief': '진행 중인 세션 요약',
  'Completed session brief': '완료된 세션 요약',
  Focus: '초점',
  Evidence: '근거',
  Discussion: '논의',
  '{{count}} {{label}}': '{{count}}개 {{label}}',
  quote: '인용',
  quotes: '인용',
  answer: '답변',
  answers: '답변',
  'persona reply': '페르소나 응답',
  'persona replies': '페르소나 응답',
  '{{count}} prompts': '{{count}}개 프롬프트',
  '{{count}} replies': '{{count}}개 응답',
  'Page {{current}} of {{target}}': '{{current}}/{{target}}쪽',
  '{{percent}}% logged': '{{percent}}% 기록됨',
  'Progress not set': '진행률 미설정',
  'No active focus': '활성 초점 없음',
  'Review locked': '리뷰 잠김',
  'Search messages and quotes': '메시지와 인용 검색',
  Clear: '지우기',
  '{{messages}} messages - {{quotes}} quotes': '{{messages}}개 메시지 - {{quotes}}개 인용',
  '{{visibleMessages}}/{{totalMessages}} messages - {{visibleQuotes}}/{{totalQuotes}} quotes': '{{visibleMessages}}/{{totalMessages}}개 메시지 - {{visibleQuotes}}/{{totalQuotes}}개 인용',
  'Reading goal for this session': '이번 세션의 독서 목표',
  'Progress note': '진행 메모',
  StartPage: '시작',
  Current: '현재',
  Target: '목표',
  'Save progress': '진행률 저장',
  'Save quote': '인용 저장',
  Page: '쪽',
  Location: '위치',
  'Quote or passage': '인용 또는 구절',
  'Why this passage matters': '이 구절이 중요한 이유',
  'Summarize what this reading session resolved': '이 독서 세션에서 정리된 내용을 요약하세요',
  Send: '보내기',
  Message: '메시지',
  'Ask book': '책에게 묻기',
  'Ask about the current reading': '현재 독서에 대해 묻기',
  'Debate personas': '페르소나 토론',
  'Challenge the current interpretation': '현재 해석에 도전하기',
  'Book answer': '책 답변',
  'Reader answer': '독자 답변',
  'Reader question': '독자 질문',
  'Persona {{id}}': '페르소나 {{id}}',
  Save: '저장',
  Cancel: '취소',
  'Export transcript': '대화 내보내기',
  'Session review': '세션 리뷰',
  '{{answers}} answers - {{quotes}} quotes - {{personaReplies}} persona replies': '{{answers}}개 답변 - {{quotes}}개 인용 - {{personaReplies}}개 페르소나 응답',
  'Progress not recorded': '진행률이 기록되지 않음',
  'Page {{current}}/{{target}}': '{{current}}/{{target}}쪽',
  'Save metric': '지표 저장',
  'Export MD': 'MD 내보내기',
  'Reading goal': '독서 목표',
  Closeout: '마무리',
  Tags: '태그',
  'Save the conclusions worth carrying forward': '계속 가져갈 결론을 저장하세요',
  'Review insights': '리뷰 인사이트',
  Theme: '주제',
  'Insight title': '인사이트 제목',
  'What should be remembered from this reading or debate?': '이 독서나 토론에서 기억해야 할 것은 무엇인가요?',
  'Evidence or passage': '근거 또는 구절',
  'Answered prompts': '답변한 프롬프트',
  'Answered question': '답변한 질문',
  'Persona response': '페르소나 응답',
  'Create a session from a candidate, then write inside the window.': '후보 책으로 세션을 만든 뒤 창 안에 기록하세요.',
  'No messages match the current search.': '현재 검색과 일치하는 메시지가 없습니다.',
  Edit: '수정',
  'Streaming response...': '응답을 스트리밍하는 중...',
  'Composer mode': '작성 모드',
  Question: '질문',
  Takeaway: '요점',
  'Save insight': '인사이트 저장',
  'Saved quotes': '저장한 인용',
  'Persona responses': '페르소나 응답',
  'Loading saved reading session...': '저장된 독서 세션을 불러오는 중...',
  Persona: '페르소나',
  'Answer the selected prompt': '선택한 프롬프트에 답변',
  'Add to the active window': '활성 창에 추가',
  'Answer selected question': '선택한 질문에 답변',
  'Window message': '창 메시지',
  'Persona debate': '페르소나 토론',
  'Persona name': '페르소나 이름',
  'Persona tone': '페르소나 어조',
  'Persona description': '페르소나 설명',
  'Persona instructions': '페르소나 지침',
  Tone: '어조',
  'What this voice watches for': '이 목소리가 주목할 점',
  'Instructions for how this persona should respond': '이 페르소나가 응답하는 방식에 대한 지침',
  'Create a voice or challenge the current interpretation': '목소리를 만들거나 현재 해석에 도전하세요',
  'Add persona': '페르소나 추가',
  'Debate with {{name}}': '{{name}}와 토론',
  'Debate all': '전체 토론',
  'Add window': '창 추가',
};

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (text: string, values?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

function readStoredLocale(): Locale {
  if (typeof window === 'undefined') {
    return 'en';
  }

  return window.localStorage.getItem(LOCALE_STORAGE_KEY) === 'ko' ? 'ko' : 'en';
}

function interpolate(text: string, values: Record<string, string | number> = {}) {
  return Object.entries(values).reduce(
    (result, [key, value]) => result.replaceAll(`{{${key}}}`, String(value)),
    text,
  );
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => readStoredLocale());

  useEffect(() => {
    document.documentElement.lang = locale;
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  }, [locale]);

  const value = useMemo<I18nContextValue>(() => ({
    locale,
    setLocale: setLocaleState,
    t(text, values) {
      const translated = locale === 'ko' ? koreanTranslations[text] || text : text;
      return interpolate(translated, values);
    },
  }), [locale]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used inside I18nProvider');
  }

  return context;
}

export function LanguageToggle() {
  const { locale, setLocale, t } = useI18n();

  return (
    <div className="inline-flex items-center gap-1 text-xs" aria-label={t('Language')} {...{ 'data-locale': locale }}>
      <button
        className={`rounded border px-2 py-1 font-medium ${locale === 'en' ? 'border-stone-900 bg-stone-900 text-white' : 'border-stone-300 bg-white text-stone-700'}`}
        onClick={() => setLocale('en')}
        type="button"
      >
        EN
      </button>
      <button
        className={`rounded border px-2 py-1 font-medium ${locale === 'ko' ? 'border-stone-900 bg-stone-900 text-white' : 'border-stone-300 bg-white text-stone-700'}`}
        onClick={() => setLocale('ko')}
        type="button"
      >
        KO
      </button>
    </div>
  );
}
