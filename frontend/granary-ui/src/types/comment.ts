// Mirrors com.example.granary.dto.CommentRequestDto
export interface CommentRequestDto {
  content: string;
  parentId?: number | null;
}

// Mirrors com.example.granary.dto.CommentVoteRequestDto
export interface CommentVoteRequestDto {
  value: 1 | -1;
}

// Mirrors com.example.granary.dto.CommentResponseDto
export interface CommentResponseDto {
  id: number;
  content: string;
  authorUsername: string;
  recipeId: number;
  parentId?: number | null;
  deleted: boolean;
  score: number;
  currentUserVote: 1 | -1 | null;
  createdAt: string;
  updatedAt: string;
  replies: CommentResponseDto[];
}
