import { CKEditor } from '@ckeditor/ckeditor5-react';
import {
  BlockQuote,
  Bold,
  ClassicEditor,
  Essentials,
  Heading,
  Italic,
  Link,
  List,
  Paragraph,
} from 'ckeditor5';
import 'ckeditor5/ckeditor5.css';

interface ReflectionRichEditorProps {
  onChange: (value: string) => void;
  placeholder: string;
  value: string;
}

export function ReflectionRichEditor({ onChange, placeholder, value }: ReflectionRichEditorProps) {
  return (
    <CKEditor
      config={{
        licenseKey: 'GPL',
        plugins: [Essentials, Paragraph, Heading, Bold, Italic, Link, List, BlockQuote],
        toolbar: ['undo', 'redo', '|', 'heading', '|', 'bold', 'italic', 'link', '|', 'bulletedList', 'numberedList', 'blockQuote'],
        placeholder,
      }}
      data={value}
      editor={ClassicEditor}
      onChange={(_, editor) => onChange(editor.getData())}
    />
  );
}
