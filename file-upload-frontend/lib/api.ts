const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type CsrfToken = {
  token: string;
  parameterName: string;
  headerName: string;
};

export type AuthUser = {
  id: number;
  username: string;
  role: string;
};

export type ApiError = {
  code: string;
  message: string;
};

export async function getCsrfToken(): Promise<CsrfToken> {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    method: "GET",
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("CSRF 토큰을 가져오지 못했습니다.");
  }

  return response.json();
}

export async function login(
  username: string,
  password: string,
): Promise<AuthUser> {
  const csrf = await getCsrfToken();

  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({
      username,
      password,
    }),
  });

  if (!response.ok) {
    const error = (await response.json()) as ApiError;

    throw new Error(
      error.message || "로그인에 실패했습니다.",
    );
  }

  return response.json();
}

export async function getMe(): Promise<AuthUser | null> {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
    method: "GET",
    credentials: "include",
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error("로그인 상태를 확인하지 못했습니다.");
  }

  return response.json();
}

export type FixedPolicy = {
  id: number;
  extension: string;
  blocked: boolean;
  updatedAt: string;
};

export async function getFixedPolicies(): Promise<FixedPolicy[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/policies/fixed`,
    {
      method: "GET",
      credentials: "include",
    },
  );

  if (response.status === 401) {
    throw new Error("UNAUTHORIZED");
  }

  if (!response.ok) {
    throw new Error("고정 확장자 정책을 불러오지 못했습니다.");
  }

  return response.json();
}

export async function updateFixedPolicy(
  policy: FixedPolicy,
  blocked: boolean,
): Promise<FixedPolicy> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/policies/fixed/${policy.id}`,
    {
      method: "PATCH",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({
        blocked,
        expectedUpdatedAt: policy.updatedAt,
        reason: null,
      }),
    },
  );

  if (!response.ok) {
    const error = (await response.json()) as ApiError;

    throw new Error(
      error.code === "POLICY_CONFLICT"
        ? "다른 관리자가 먼저 정책을 변경했습니다. 최신 상태를 다시 불러옵니다."
        : error.message || "정책 변경에 실패했습니다.",
    );
  }

  return response.json();
}

export type CustomPolicy = {
  id: number;
  extension: string;
  blocked: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CustomPolicyPage = {
  items: CustomPolicy[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  totalCustomCount: number;
};

export async function getCustomPolicies(
  page = 0,
  size = 20,
  search = "",
): Promise<CustomPolicyPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (search.trim()) {
    params.set("search", search.trim());
  }

  const response = await fetch(
    `${API_BASE_URL}/api/policies/custom?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
    },
  );

  if (response.status === 401) {
    throw new Error("UNAUTHORIZED");
  }

  if (!response.ok) {
    throw new Error(
      "커스텀 확장자 정책을 불러오지 못했습니다.",
    );
  }

  return response.json();
}

export async function createCustomPolicy(
  extension: string,
  reason: string,
): Promise<CustomPolicy> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/policies/custom`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({
        extension,
        reason,
      }),
    },
  );

  if (!response.ok) {
    const error = (await response.json()) as ApiError;

    throw new Error(
      error.message || "확장자 추가에 실패했습니다.",
    );
  }

  return response.json();
}

export async function deleteCustomPolicy(
  policy: CustomPolicy,
): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/policies/custom/${policy.id}`,
    {
      method: "DELETE",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({
        expectedUpdatedAt: policy.updatedAt,
        reason: "관리 화면에서 커스텀 확장자 삭제",
      }),
    },
  );

  if (!response.ok) {
    const error = (await response.json()) as ApiError;

    throw new Error(
      error.code === "POLICY_CONFLICT"
        ? "다른 관리자가 먼저 정책을 변경했습니다. 최신 상태를 다시 불러옵니다."
        : error.message || "확장자 삭제에 실패했습니다.",
    );
  }
}

export type FileUploadResponse = {
  id: string;
  originalFilename: string;
  extension: string;
  detectedMimeType: string;
  sizeBytes: number;
  sha256: string;
  status: string;
};

export class UploadRequestError extends Error {
  status: number | null;
  code: string | null;

  constructor(
    status: number | null,
    code: string | null,
    message: string,
  ) {
    super(message);
    this.name = "UploadRequestError";
    this.status = status;
    this.code = code;
  }
}

export async function uploadFileWithProgress(
  file: File,
  onProgress: (progress: number) => void,
): Promise<FileUploadResponse> {
  const csrf = await getCsrfToken();

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.open(
      "POST",
      `${API_BASE_URL}/api/files`,
    );

    xhr.withCredentials = true;

    xhr.setRequestHeader(
      csrf.headerName,
      csrf.token,
    );

    xhr.upload.addEventListener(
      "progress",
      (event) => {
        if (!event.lengthComputable) {
          return;
        }

        const progress = Math.round(
          (event.loaded / event.total) * 100,
        );

        onProgress(progress);
      },
    );

    xhr.addEventListener("load", () => {
      if (
        xhr.status >= 200 &&
        xhr.status < 300
      ) {
        try {
          resolve(
            JSON.parse(
              xhr.responseText,
            ) as FileUploadResponse,
          );
        } catch {
          reject(
            new UploadRequestError(
              xhr.status,
              "INVALID_RESPONSE",
              "서버 응답을 처리하지 못했습니다.",
            ),
          );
        }

        return;
      }

      let apiError: ApiError | null = null;

      try {
        apiError = JSON.parse(
          xhr.responseText,
        ) as ApiError;
      } catch {
        // JSON 오류 응답이 아닌 경우 아래 기본 메시지 사용
      }

      reject(
        new UploadRequestError(
          xhr.status,
          apiError?.code ?? null,
          apiError?.message ??
            "파일 업로드에 실패했습니다.",
        ),
      );
    });

    xhr.addEventListener("error", () => {
      reject(
        new UploadRequestError(
          null,
          "NETWORK_ERROR",
          "네트워크 오류가 발생했습니다.",
        ),
      );
    });

    xhr.addEventListener("abort", () => {
      reject(
        new UploadRequestError(
          null,
          "UPLOAD_ABORTED",
          "파일 업로드가 취소되었습니다.",
        ),
      );
    });

    const formData = new FormData();
    formData.append("file", file);

    xhr.send(formData);
  });
}

export async function logout(): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/auth/logout`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (!response.ok) {
    throw new Error("로그아웃에 실패했습니다.");
  }
}