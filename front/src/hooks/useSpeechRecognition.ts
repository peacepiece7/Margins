import { useEffect, useMemo, useRef, useState } from 'react';
import {
  appendTranscript,
  collectFinalTranscript,
  getSpeechRecognitionConstructor,
  type SpeechRecognitionLike,
} from '../utils/speechRecognition';

interface UseSpeechRecognitionOptions {
  language?: string;
  onTranscript: (value: string) => void;
  value: string;
}

export function useSpeechRecognition({
  language = 'ko-KR',
  onTranscript,
  value,
}: UseSpeechRecognitionOptions) {
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const valueRef = useRef(value);
  const onTranscriptRef = useRef(onTranscript);
  const [error, setError] = useState<string | null>(null);
  const [listening, setListening] = useState(false);

  const Recognition = useMemo(() => getSpeechRecognitionConstructor(), []);
  const supported = Boolean(Recognition);

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  useEffect(() => {
    onTranscriptRef.current = onTranscript;
  }, [onTranscript]);

  useEffect(() => () => {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
  }, []);

  function stop() {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    setListening(false);
  }

  function start() {
    if (!Recognition) {
      setError('이 브라우저는 음성 입력을 지원하지 않습니다.');
      return;
    }

    if (recognitionRef.current) {
      stop();
      return;
    }

    const recognition = new Recognition();
    recognition.continuous = true;
    recognition.interimResults = false;
    recognition.lang = language;
    recognition.onresult = (event) => {
      const transcript = collectFinalTranscript(event);
      if (!transcript) {
        return;
      }

      const nextValue = appendTranscript(valueRef.current, transcript);
      valueRef.current = nextValue;
      onTranscriptRef.current(nextValue);
    };
    recognition.onerror = (event) => {
      setError(event.error === 'not-allowed' ? '마이크 권한이 필요합니다.' : '음성 입력을 다시 시도해 주세요.');
      stop();
    };
    recognition.onend = () => {
      recognitionRef.current = null;
      setListening(false);
    };

    try {
      recognition.start();
      recognitionRef.current = recognition;
      setError(null);
      setListening(true);
    } catch {
      recognitionRef.current = null;
      setListening(false);
      setError('음성 입력을 시작하지 못했습니다.');
    }
  }

  return {
    error,
    listening,
    start,
    stop,
    supported,
    toggle: listening ? stop : start,
  };
}
