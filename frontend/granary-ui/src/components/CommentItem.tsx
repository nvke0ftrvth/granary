import { useState } from 'react';
import { useSelector } from 'react-redux';
import { format } from 'date-fns';
import type { RootState } from '../store';
import type { CommentResponseDto } from '../types/comment';
import {
  useVoteCommentMutation,
  useRemoveVoteMutation,
  useDeleteCommentMutation,
  useUpdateCommentMutation,
  useCreateCommentMutation,
} from '../store/commentApi';

interface CommentItemProps {
  comment: CommentResponseDto;
  recipeId: number;
  depth?: number;
}

export function CommentItem({ comment, recipeId, depth = 0 }: CommentItemProps) {
  const username = useSelector((state: RootState) => state.auth.username);
  const isLoggedIn = Boolean(username);
  const isOwner = username != null && username === comment.authorUsername;

  const [voteComment] = useVoteCommentMutation();
  const [removeVote] = useRemoveVoteMutation();
  const [deleteComment] = useDeleteCommentMutation();
  const [updateComment] = useUpdateCommentMutation();
  const [createComment, { isLoading: isReplyPosting }] = useCreateCommentMutation();

  const [isReplying, setIsReplying] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(comment.content);

  const handleVote = (value: 1 | -1) => {
    if (comment.currentUserVote === value) {
      removeVote({ id: comment.id, recipeId });
    } else {
      voteComment({ id: comment.id, recipeId, value });
    }
  };

  const handleReplySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyText.trim()) return;
    await createComment({ recipeId, body: { content: replyText, parentId: comment.id } });
    setReplyText('');
    setIsReplying(false);
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editText.trim()) return;
    await updateComment({ id: comment.id, recipeId, body: { content: editText } });
    setIsEditing(false);
  };

  const handleDelete = () => {
    if (window.confirm('Delete this comment?')) {
      deleteComment({ id: comment.id, recipeId });
    }
  };

  return (
    <div className="comment" style={depth > 0 ? { marginLeft: '1.5rem' } : undefined}>
      <div className="comment-meta">
        <span className="comment-author">{comment.authorUsername}</span>
        <span className="comment-date">{format(new Date(comment.createdAt), 'yyyy/MM/dd HH:mm')}</span>
      </div>

      {isEditing ? (
        <form onSubmit={handleEditSubmit} className="comment-edit-form">
          <textarea value={editText} onChange={(e) => setEditText(e.target.value)} maxLength={2000} />
          <div className="comment-form-actions">
            <button type="submit" className="submit-btn">
              Save
            </button>
            <button type="button" className="cancel-btn" onClick={() => setIsEditing(false)}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <p className={`comment-content ${comment.deleted ? 'comment-deleted' : ''}`}>{comment.content}</p>
      )}

      <div className="comment-actions">
        <div className="comment-votes">
          <button
            type="button"
            className={`vote-btn ${comment.currentUserVote === 1 ? 'active' : ''}`}
            onClick={() => handleVote(1)}
            disabled={!isLoggedIn || comment.deleted}
            aria-label="Upvote"
          >
            ▲
          </button>
          <span className="vote-score">{comment.score}</span>
          <button
            type="button"
            className={`vote-btn ${comment.currentUserVote === -1 ? 'active' : ''}`}
            onClick={() => handleVote(-1)}
            disabled={!isLoggedIn || comment.deleted}
            aria-label="Downvote"
          >
            ▼
          </button>
        </div>

        {isLoggedIn && !comment.deleted && (
          <button type="button" className="comment-link-btn" onClick={() => setIsReplying((v) => !v)}>
            Reply
          </button>
        )}

        {isOwner && !comment.deleted && (
          <>
            <button type="button" className="comment-link-btn" onClick={() => setIsEditing((v) => !v)}>
              Edit
            </button>
            <button type="button" className="comment-link-btn comment-delete-btn" onClick={handleDelete}>
              Delete
            </button>
          </>
        )}
      </div>

      {isReplying && (
        <form onSubmit={handleReplySubmit} className="comment-reply-form">
          <textarea
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder="Write a reply…"
            maxLength={2000}
          />
          <div className="comment-form-actions">
            <button type="submit" className="submit-btn" disabled={isReplyPosting || !replyText.trim()}>
              Reply
            </button>
            <button type="button" className="cancel-btn" onClick={() => setIsReplying(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {comment.replies && comment.replies.length > 0 && (
        <div className="comment-replies">
          {comment.replies.map((reply) => (
            <CommentItem key={reply.id} comment={reply} recipeId={recipeId} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}
