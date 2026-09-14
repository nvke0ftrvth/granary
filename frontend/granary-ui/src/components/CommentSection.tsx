import { useState } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';
import { useGetCommentsQuery, useCreateCommentMutation } from '../store/commentApi';
import { CommentItem } from './CommentItem';

interface CommentSectionProps {
  recipeId: number;
}

export function CommentSection({ recipeId }: CommentSectionProps) {
  const username = useSelector((state: RootState) => state.auth.username);
  const isLoggedIn = Boolean(username);
  const { data: comments, isLoading, error } = useGetCommentsQuery(recipeId);
  const [createComment, { isLoading: isPosting }] = useCreateCommentMutation();
  const [newComment, setNewComment] = useState('');
  const [isVisible, setIsVisible] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    await createComment({ recipeId, body: { content: newComment } });
    setNewComment('');
  };

  return (
    <section className="comment-section">
      <h4>Comments</h4>

      {!isVisible ? (
        <button type="button" className="comment-section-toggle" onClick={() => setIsVisible(true)}>
          Show Comments{comments ? ` (${comments.length})` : ''}
        </button>
      ) : (
        <>
          <button type="button" className="comment-section-toggle" onClick={() => setIsVisible(false)}>
            Hide Comments
          </button>

          {isLoggedIn && (
            <form onSubmit={handleSubmit} className="comment-new-form">
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder="Leave a comment…"
                maxLength={2000}
              />
              <button type="submit" className="submit-btn" disabled={isPosting || !newComment.trim()}>
                Post comment
              </button>
            </form>
          )}

          {isLoading && <p className="status-message">Loading comments…</p>}
          {error && <p className="status-message form-error">Couldn't load comments.</p>}
          {comments && comments.length === 0 && <p className="status-message">No comments yet.</p>}

          <div className="comment-list">
            {comments?.map((comment) => (
              <CommentItem key={comment.id} comment={comment} recipeId={recipeId} />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
