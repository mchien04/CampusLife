# Sequence Diagram: Chat với Trợ lý AI (Chatbot)

## Hệ thống: CampusLife (Spring Boot + React)

### Endpoint: `POST /api/chatbot`

---

```mermaid
sequenceDiagram
    autonumber

    actor U as Student/Admin
    participant C as Client (React)
    participant CT as ChatbotController
    participant CS as ChatbotService
    participant CR as ChatSessionRepository
    participant DB as Database
    participant VD as VectorDB
    participant AI as AI/LLMService

    Note over U, AI: Luồng 1: Người dùng gửi câu hỏi

    U->>C: Nhập câu hỏi (message, sessionId)
    activate C
    C->>C: Kiểm tra/validate input
    C->>CT: POST /api/chatbot<br/>{ message, sessionId, timestamp }
    deactivate C

    activate CT
    CT->>CT: Validate request body<br/>(@Valid ChatRequestDTO)
    CT->>CS: processChat(ChatRequestDTO)
    deactivate CT

    activate CS

    Note over CS: Luồng 2: Xử lý Session & Lịch sử

    alt sessionId tồn tại
        CS->>CR: findBySessionId(sessionId)
        activate CR
        CR->>DB: SELECT * FROM chat_sessions WHERE session_id = ?
        activate DB
        DB-->>CR: ChatSession entity
        deactivate DB
        CR-->>CS: Optional<ChatSession>
        deactivate CR
    else sessionId null hoặc không tồn tại
        CS->>CS: Tạo sessionId mới (UUID)
        CS->>CR: save(new ChatSession(sessionId, userId, createdAt))
        activate CR
        CR->>DB: INSERT INTO chat_sessions (...)
        activate DB
        DB-->>CR: ChatSession persisted
        deactivate DB
        CR-->>CS: ChatSession
        deactivate CR
    end

    CS->>CR: findRecentMessagesBySessionId(sessionId, limit=N)
    activate CR
    CR->>DB: SELECT * FROM chat_messages<br/>WHERE session_id = ?<br/>ORDER BY created_at DESC LIMIT N
    activate DB
    DB-->>CR: List<ChatMessage> (last N messages)
    deactivate DB
    CR-->>CS: List<ChatMessage>
    deactivate CR

    CS->>CS: Xây dựng context:<br/>- System prompt (vai trò trợ lý campus)<br/>- History (last N messages)<br/>- Current message

    Note over CS, AI: Luồng 3: Xử lý AI Response (alt: Direct LLM vs RAG)

    alt RAG Enabled (Retrieval Augmented Generation)
        Note over CS, VD: RAG Path: Tìm kiếm thông tin bổ sung

        CS->>VD: searchSimilarDocuments(query=message, topK=K)
        activate VD
        VD->>VD: Embedding query vector (vectorize)
        VD->>VD: Cosine similarity search<br/>trong document chunks
        VD-->>CS: List<RelevantChunk> (top K chunks)<br/>[quy định, hoạt động, FAQ, ...]
        deactivate VD

        CS->>CS: Enrich context với RAG:<br/>- System prompt<br/>- History<br/>- Retrieved context (top K chunks)<br/>- Current message + "Dựa trên thông tin sau: ..."

        CS->>AI: sendChatRequest(context enriched)
        activate AI
        AI->>AI: Generate response<br/>(OpenAI / Gemini / Claude API)<br/>dựa trên context + retrieved chunks
        AI-->>CS: AIResponse { content, tokens, model }
        deactivate AI

    else Direct LLM (No RAG)
        Note over CS, AI: Direct LLM Path: Gọi AI trực tiếp

        CS->>AI: sendChatRequest(context basic)
        activate AI
        AI->>AI: Generate response<br/>(OpenAI / Gemini / Claude API)<br/>dựa trên context + history
        AI-->>CS: AIResponse { content, tokens, model }
        deactivate AI
    end

    Note over CS, DB: Luồng 4: Lưu trữ & Trả về

    CS->>CS: Extract response text from AIResponse

    CS->>CR: saveUserMessage(sessionId, userId, message, timestamp)
    activate CR
    CR->>DB: INSERT INTO chat_messages<br/>(session_id, sender, content, created_at)
    activate DB
    DB-->>CR: ChatMessage persisted
    deactivate DB
    CR-->>CS: ChatMessage
    deactivate CR

    CS->>CR: saveAIMessage(sessionId, "AI", responseContent, timestamp)
    activate CR
    CR->>DB: INSERT INTO chat_messages<br/>(session_id, sender, content, created_at)
    activate DB
    DB-->>CR: ChatMessage persisted
    deactivate DB
    CR-->>CS: ChatMessage
    deactivate CR

    CS->>CS: Build ChatResponseDTO<br/>{ sessionId, reply, timestamp, model, tokensUsed }

    CS-->>CT: ChatResponseDTO
    deactivate CS

    activate CT
    CT-->>C: ResponseEntity<ChatResponseDTO> (200 OK)
    deactivate CT

    activate C
    C->>C: Update UI state<br/>Append AI message to chat history
    C-->>U: Hiển thị reply AI + sessionId
    deactivate C

    Note over U, AI: Kết thúc luồng xử lý chat
```

---

## Tóm tắt Thành phần và Chức năng

### Thành phần tham gia

| Thành phần | Vai trò | Chức năng chính |
|---|---|---|
| **Student/Admin** | Actor | Người dùng cuối nhập câu hỏi và nhận phản hồi từ chatbot. |
| **Client (React)** | Frontend | UI chat interface, validate input, gửi request HTTP POST, nhận response và render tin nhắn AI. |
| **ChatbotController** | Controller Layer | Nhận request `POST /api/chatbot`, validate DTO, điều phối đến ChatbotService, trả về ResponseEntity. |
| **ChatbotService** | Service Layer | Xử lý nghiệp vụ chính: quản lý session, lấy lịch sử, xây dựng context, gọi AI (có hoặc không RAG), lưu message vào DB. |
| **ChatSessionRepository** | Repository Layer | Tầng truy cập dữ liệu cho ChatSession và ChatMessage, thực hiện CRUD và truy vấn lịch sử. |
| **Database** | Persistence | Lưu trữ bền vững chat sessions, chat messages, và metadata. |
| **VectorDB** | External Storage | Chỉ mục vector của tài liệu nội bộ (quy định, FAQ, hoạt động). Dùng cho tìm kiếm semantic similarity trong RAG. |
| **AI/LLMService** | External Service | Giao tiếp với API của OpenAI / Gemini / Claude. Nhận context và trả về generated response. |

### Luồng xử lý chính

1. **Tiếp nhận & Validation**: Client kiểm tra input và gửi HTTP POST đến Controller. Controller validate DTO trước khi chuyển xuống Service.

2. **Session Management**: Service kiểm tra `sessionId`. Nếu tồn tại → tải session cũ; nếu không → tạo session mới và lưu vào DB.

3. **Lịch sử & Context**: Lấy `N` tin nhắn gần nhất từ DB để xây dựng conversation history. Kết hợp system prompt + history + current message thành context cho AI.

4. **AI Processing (alt block)**:
   - **RAG Path**: Truy vấn VectorDB để lấy top K relevant document chunks, enrich context với retrieved information, sau đó gọi LLM.
   - **Direct LLM Path**: Gọi LLM trực tiếp với context cơ bản (system prompt + history + current message).

5. **Persistence**: Lưu song song cả user message và AI response vào DB qua Repository để đảm bảo lịch sử chat đầy đủ.

6. **Phản hồi**: Trả về DTO chứa reply, sessionId, metadata cho Client. Client cập nhật UI và hiển thị cho người dùng.

### Biến thể (alt) trong diagram

- **Session Handling**: `sessionId` tồn tại vs. tạo mới.
- **AI Strategy**: `RAG Enabled` vs. `Direct LLM`, cho phép hệ thống linh hoạt sử dụng retrieval augmentation khi cần thông tin chính xác từ tài liệu nội bộ, hoặc gọi LLM trực tiếp cho các câu hỏi tổng quát.

---

*Generated for CampusLife System | Chatbot Module (Trợ lý AI)*
