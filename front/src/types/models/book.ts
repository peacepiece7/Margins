export interface BookCandidate {
  candidateId: string;
  isbn?: string;
  isbn10?: string;
  isbn13?: string;
  title: string;
  subtitle?: string;
  author: string;
  authors?: string[];
  publisher?: string;
  publishedDate?: string;
  publishedYear?: number;
  description?: string;
  thumbnail?: string;
  language?: string;
  pageCount?: number;
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
