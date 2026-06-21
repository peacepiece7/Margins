export interface BookCandidate {
  candidateId: string;
  title: string;
  author: string;
  publishedYear?: number;
  reason?: string;
}

export interface BookCandidateSearchResponse {
  aiModel: string;
  candidates: BookCandidate[];
}

export interface SaveBookResponse {
  bookId: number;
  title: string;
  author: string;
}

export interface BookListResponse {
  books: SaveBookResponse[];
}
