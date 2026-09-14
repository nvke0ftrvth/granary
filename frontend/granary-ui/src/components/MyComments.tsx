import { format } from 'date-fns';
import type { CommentResponseDto } from '../types/comment';

interface MyCommentsProps {
  comments: CommentResponseDto[];
}

export function MyComments({ comments }: MyCommentsProps) {
  if (comments.length === 0) {
    return <p className="status-message">You haven't written any comments yet.</p>;
  }

  return (
    <ul className="my-comments-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment">
          <div className="comment-meta">
            <span className="comment-author">On {comment.recipeTitle}</span>
            <span className="comment-date">{format(new Date(comment.createdAt), 'yyyy/MM/dd HH:mm')}</span>
          </div>
          <p className="comment-content">{comment.content}</p>
        </li>
      ))}
    </ul>
  );
}
