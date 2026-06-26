import Image from '@tiptap/extension-image';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import DOMPurify from 'dompurify';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { ReadingSessionReview, ReadingSessionReviewStatus, SaveReadingSessionReviewRequest } from '../../types/models/session';
import { testAttr } from '../../utils/testAttrs';

interface ReviewPostEditorProps {
  bookTitle?: string;
  loading: boolean;
  review?: ReadingSessionReview;
  sessionId: number;
  onSave: (review: SaveReadingSessionReviewRequest) => Promise<boolean>;
}

const emptyContent = '<p></p>';

function defaultTitle(bookTitle?: string) {
  return bookTitle ? `${bookTitle} review` : 'Reading review';
}

function isSupportedImageUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function ReviewPostEditor({ bookTitle, loading, review, onSave, sessionId }: ReviewPostEditorProps) {
  const [editing, setEditing] = useState(!review);
  const [title, setTitle] = useState(review?.title || defaultTitle(bookTitle));
  const [status, setStatus] = useState<ReadingSessionReviewStatus>(review?.status === 'published' ? 'published' : 'draft');
  const [editorEmpty, setEditorEmpty] = useState(!review?.contentHtml);
  const [imageError, setImageError] = useState<string | undefined>();
  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({
        allowBase64: false,
        HTMLAttributes: {
          class: 'review-post-image',
        },
      }),
    ],
    content: review?.contentHtml || emptyContent,
    editable: !loading,
    editorProps: {
      attributes: {
        'aria-label': 'Review post body',
        class: 'review-post-editor prose max-w-none focus:outline-none',
      },
    },
    onUpdate({ editor }) {
      setEditorEmpty(editor.isEmpty);
    },
  });
  const hasReview = Boolean(review?.reviewId);
  const saveLabel = hasReview ? 'Save edits' : 'Save post';
  const previewHtml = useMemo(() => DOMPurify.sanitize(review?.contentHtml || ''), [review?.contentHtml]);

  useEffect(() => {
    editor?.setEditable(!loading);
  }, [editor, loading]);

  useEffect(() => {
    setEditing(!review);
    setTitle(review?.title || defaultTitle(bookTitle));
    setStatus(review?.status === 'published' ? 'published' : 'draft');
    editor?.commands.setContent(review?.contentHtml || emptyContent);
    setEditorEmpty(!review?.contentHtml);
    setImageError(undefined);
  }, [bookTitle, editor, review, review?.contentHtml, review?.reviewId, review?.status, review?.title, sessionId]);

  function addImage() {
    const src = window.prompt('Image URL');
    if (!src?.trim()) {
      return;
    }

    const imageUrl = src.trim();
    if (!isSupportedImageUrl(imageUrl)) {
      setImageError('Use an http or https image URL.');
      return;
    }

    setImageError(undefined);
    editor?.chain().focus().setImage({ src: imageUrl }).run();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const contentHtml = editor?.getHTML() || '';
    if (!title.trim() || !editor || editor.isEmpty) {
      return;
    }

    void onSave({
      title: title.trim(),
      contentHtml,
      status,
    }).then((saved) => {
      if (saved) {
        setEditing(false);
      }
    });
  }

  if (!editing && review) {
    return (
      <section className="grid gap-3 border-b border-stone-200 bg-stone-50 px-4 py-4" {...testAttr('review-post')}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs font-semibold uppercase text-stone-500">Review post</div>
            <h2 className="mt-1 text-xl font-semibold leading-7 text-stone-950" {...testAttr('review-post-title')}>
              {review.title}
            </h2>
            <div className="mt-1 text-xs text-stone-500">
              {review.editorType || 'tiptap-free'} - {review.status || 'draft'}
            </div>
          </div>
          <button
            className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
            disabled={loading}
            onClick={() => setEditing(true)}
            type="button"
            {...testAttr('review-post-edit')}
          >
            Edit post
          </button>
        </div>
        <article
          className="review-post-preview rounded border border-stone-200 bg-white px-4 py-4 text-sm leading-7 text-stone-800"
          dangerouslySetInnerHTML={{ __html: previewHtml }}
          {...testAttr('review-post-preview')}
        />
      </section>
    );
  }

  return (
    <section className="grid gap-3 border-b border-stone-200 bg-stone-50 px-4 py-4" {...testAttr('review-post-editor')}>
      <div>
        <div className="text-xs font-semibold uppercase text-stone-500">Review post editor</div>
        <div className="mt-1 text-sm font-medium text-stone-900">
          {bookTitle ? `Write the post for ${bookTitle}` : 'Write the reading review post'}
        </div>
      </div>
      <form className="grid gap-3" onSubmit={submit}>
        <div className="grid gap-2 md:grid-cols-[1fr_150px]">
          <input
            aria-label="Review post title"
            className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
            disabled={loading}
            maxLength={255}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Post title"
            value={title}
            {...testAttr('review-post-title-input')}
          />
          <select
            aria-label="Review post status"
            className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
            disabled={loading}
            onChange={(event) => setStatus(event.target.value === 'published' ? 'published' : 'draft')}
            value={status}
            {...testAttr('review-post-status')}
          >
            <option value="draft">Draft</option>
            <option value="published">Published</option>
          </select>
        </div>

        <div className="rounded border border-stone-300 bg-white" {...testAttr('review-post-editor-surface')}>
          <div className="flex flex-wrap gap-1 border-b border-stone-200 bg-stone-50 p-2">
            <ToolbarButton active={editor?.isActive('bold')} disabled={loading} label="Bold" onClick={() => editor?.chain().focus().toggleBold().run()} />
            <ToolbarButton active={editor?.isActive('italic')} disabled={loading} label="Italic" onClick={() => editor?.chain().focus().toggleItalic().run()} />
            <ToolbarButton active={editor?.isActive('heading', { level: 2 })} disabled={loading} label="H2" onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()} />
            <ToolbarButton active={editor?.isActive('bulletList')} disabled={loading} label="List" onClick={() => editor?.chain().focus().toggleBulletList().run()} />
            <ToolbarButton active={editor?.isActive('blockquote')} disabled={loading} label="Quote" onClick={() => editor?.chain().focus().toggleBlockquote().run()} />
            <ToolbarButton disabled={loading} label="Image" onClick={addImage} />
          </div>
          <EditorContent editor={editor} />
          <div className="border-t border-stone-200 bg-stone-50 px-3 py-2 text-xs leading-5 text-stone-500">
            Images use external http/https URLs in this MVP. Direct image uploads are a later storage feature.
          </div>
          {imageError && (
            <div
              aria-live="polite"
              className="border-t border-amber-200 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-800"
              role="alert"
              {...testAttr('review-post-image-error')}
            >
              {imageError}
            </div>
          )}
        </div>

        <div className="flex flex-wrap justify-end gap-2">
          {hasReview && (
            <button
              className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600 disabled:opacity-50"
              disabled={loading}
              onClick={() => {
                setEditing(false);
                setTitle(review?.title || defaultTitle(bookTitle));
                setStatus(review?.status === 'published' ? 'published' : 'draft');
                editor?.commands.setContent(review?.contentHtml || emptyContent);
                setEditorEmpty(!review?.contentHtml);
                setImageError(undefined);
              }}
              type="button"
              {...testAttr('review-post-cancel')}
            >
              Cancel
            </button>
          )}
          <button
            className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
            disabled={loading || !title.trim() || !editor || editorEmpty}
            type="submit"
            {...testAttr('review-post-save')}
          >
            {saveLabel}
          </button>
        </div>
      </form>
    </section>
  );
}

function ToolbarButton({
  active,
  disabled,
  label,
  onClick,
}: {
  active?: boolean;
  disabled?: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      className={`min-h-8 rounded border px-2 py-1 text-xs font-medium ${
        active ? 'border-stone-900 bg-stone-900 text-white' : 'border-stone-300 bg-white text-stone-700'
      } disabled:opacity-50`}
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}
