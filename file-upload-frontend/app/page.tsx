"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { useRouter } from "next/navigation";
import FileUploadSection from "@/components/FileUploadSection";

import {
  AuthUser,
  CustomPolicy,
  FixedPolicy,
  createCustomPolicy,
  deleteCustomPolicy,
  getCustomPolicies,
  getFixedPolicies,
  getMe,
  logout,
  updateFixedPolicy,
} from "@/lib/api";

export default function HomePage() {
  const router = useRouter();

  const [user, setUser] = useState<AuthUser | null>(null);
  const [policies, setPolicies] = useState<FixedPolicy[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [savingIds, setSavingIds] = useState<Set<number>>(
    new Set(),
  );

  const [fixedErrorMessage, setFixedErrorMessage] =
  useState<string | null>(null);

  const [customErrorMessage, setCustomErrorMessage] =
  useState<string | null>(null);

  const [customPolicies, setCustomPolicies] =
  useState<CustomPolicy[]>([]);

  const [customExtension, setCustomExtension] =
    useState("");

  const [customReason, setCustomReason] =
    useState("");

  const [customPage, setCustomPage] =
    useState(0);

  const [customTotalPages, setCustomTotalPages] =
    useState(0);

  const [customTotalCount, setCustomTotalCount] =
    useState(0);

  const [isAddingCustom, setIsAddingCustom] =
    useState(false);

  const [deletingIds, setDeletingIds] =
    useState<Set<number>>(new Set());

  const [isLoggingOut, setIsLoggingOut] =
  useState(false);

  const loadPolicies = useCallback(
  async () => {
    const result =
      await getFixedPolicies();

    setPolicies(result);
  },
  [],
);

const loadCustomPolicies = useCallback(
  async (page: number) => {
    const result =
      await getCustomPolicies(
        page,
        20,
      );

    setCustomPolicies(result.items);
    setCustomPage(result.page);
    setCustomTotalPages(
      result.totalPages,
    );
    setCustomTotalCount(
      result.totalCustomCount,
    );
  },
  [],
);

  useEffect(() => {
    async function initialize() {
      try {
        const currentUser = await getMe();

        if (!currentUser) {
          router.replace("/login");
          return;
        }

        setUser(currentUser);

        await loadPolicies();
        await loadCustomPolicies(0);
      } catch (error) {
        if (
          error instanceof Error &&
          error.message === "UNAUTHORIZED"
        ) {
          router.replace("/login");
          return;
        }

        setFixedErrorMessage(
          error instanceof Error
            ? error.message
            : "데이터를 불러오지 못했습니다.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    initialize();
  }, [
    router,
    loadPolicies,
    loadCustomPolicies,
  ]);

  async function handleToggle(policy: FixedPolicy) {
    if (savingIds.has(policy.id)) {
      return;
    }

    setFixedErrorMessage(null);

    setSavingIds((current) => {
      const next = new Set(current);
      next.add(policy.id);
      return next;
    });

    try {
      const updated = await updateFixedPolicy(
        policy,
        !policy.blocked,
      );

      setPolicies((current) =>
        current.map((item) =>
          item.id === updated.id ? updated : item,
        ),
      );
    } catch (error) {
      setFixedErrorMessage(
        error instanceof Error
          ? error.message
          : "정책 변경에 실패했습니다.",
      );

      try {
        await loadPolicies();
      } catch {
        // 기존 오류 메시지를 유지한다.
      }
    } finally {
      setSavingIds((current) => {
        const next = new Set(current);
        next.delete(policy.id);
        return next;
      });
    }
  }

  async function handleAddCustom() {
  const extension = customExtension.trim();

  if (!extension) {
    setCustomErrorMessage(
      "추가할 확장자를 입력해주세요.",
    );
    return;
  }

  if (!customReason.trim()) {
    setCustomErrorMessage(
      "추가 사유를 입력해주세요.",
    );
    return;
  }

  setIsAddingCustom(true);
  setCustomErrorMessage(null);

  try {
    await createCustomPolicy(
      extension,
      customReason.trim(),
    );

    setCustomExtension("");
    setCustomReason("");

    await loadCustomPolicies(0);
  } catch (error) {
    setCustomErrorMessage(
      error instanceof Error
        ? error.message
        : "확장자 추가에 실패했습니다.",
    );
  } finally {
    setIsAddingCustom(false);
  }
}

  async function handleDeleteCustom(
    policy: CustomPolicy,
  ) {
    if (deletingIds.has(policy.id)) {
      return;
    }

    setCustomErrorMessage(null);

    setDeletingIds((current) => {
      const next = new Set(current);
      next.add(policy.id);
      return next;
    });

    try {
      await deleteCustomPolicy(policy);

      /*
      * 현재 페이지의 마지막 항목을 삭제했다면
      * 이전 페이지로 이동한다.
      */
      const nextPage =
        customPolicies.length === 1 &&
        customPage > 0
          ? customPage - 1
          : customPage;

      await loadCustomPolicies(nextPage);
    } catch (error) {
      setCustomErrorMessage(
        error instanceof Error
          ? error.message
          : "확장자 삭제에 실패했습니다.",
      );

      try {
        await loadCustomPolicies(customPage);
      } catch {
        // 기존 오류를 유지한다.
      }
    } finally {
      setDeletingIds((current) => {
        const next = new Set(current);
        next.delete(policy.id);
        return next;
      });
    }
  }

  async function handleLogout() {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);

    try {
      await logout();

      router.replace("/login");
      router.refresh();
    } catch (error) {
      setFixedErrorMessage(
        error instanceof Error
          ? error.message
          : "로그아웃에 실패했습니다.",
      );

      setIsLoggingOut(false);
    }
  }

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50">
        <p className="text-sm text-slate-500">
          불러오는 중...
        </p>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10">
      <div className="mx-auto max-w-4xl">
        <header className="mb-8 flex items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">
              파일 업로드 관리
            </h1>

            <p className="mt-2 text-sm text-slate-500">
              {user.username} / {user.role}
            </p>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            disabled={isLoggingOut}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isLoggingOut
              ? "로그아웃 중..."
              : "로그아웃"}
          </button>
        </header>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-900">
              고정 확장자
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              체크한 확장자는 실제 파일 업로드 시
              차단됩니다.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            {policies.map((policy) => {
              const isSaving = savingIds.has(policy.id);

              return (
                <label
                  key={policy.id}
                  className={[
                    "flex cursor-pointer items-center gap-2 rounded-lg border px-4 py-3 transition",
                    policy.blocked
                      ? "border-slate-900 bg-slate-900 text-white"
                      : "border-slate-300 bg-white text-slate-700 hover:bg-slate-50",
                    isSaving
                      ? "cursor-wait opacity-60"
                      : "",
                  ].join(" ")}
                >
                  <input
                    type="checkbox"
                    checked={policy.blocked}
                    disabled={isSaving}
                    onChange={() => handleToggle(policy)}
                    className="h-4 w-4"
                  />

                  <span className="font-mono text-sm">
                    {policy.extension}
                  </span>
                </label>
              );
            })}
          </div>

          {fixedErrorMessage && (
            <div
              role="alert"
              className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {fixedErrorMessage}
            </div>
          )}
        </section>
        <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6 flex items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                커스텀 확장자
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                직접 차단할 확장자를 추가할 수 있습니다.
              </p>
            </div>

            <span className="shrink-0 rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
              {customTotalCount} / 200
            </span>
          </div>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={customExtension}
                maxLength={20}
                disabled={
                  isAddingCustom ||
                  customTotalCount >= 200
                }
                onChange={(event) =>
                  setCustomExtension(
                    event.target.value,
                  )
                }
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    handleAddCustom();
                  }
                }}
                placeholder="확장자 입력 (예: sh)"
                className="w-full rounded-lg border border-slate-300 px-3 py-2.5 pr-16 text-slate-900 outline-none focus:border-slate-500 focus:ring-2 focus:ring-slate-200 disabled:bg-slate-100"
              />

              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400">
                {customExtension.length}/20
              </span>
            </div>

            <button
              type="button"
              onClick={handleAddCustom}
              disabled={
                isAddingCustom ||
                customTotalCount >= 200
              }
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              {isAddingCustom
                ? "추가 중..."
                : "추가"}
            </button>
          </div>

          <div className="mt-3">
            <input
              type="text"
              value={customReason}
              maxLength={200}
              disabled={isAddingCustom}
              onChange={(event) =>
                setCustomReason(
                  event.target.value,
                )
              }
              placeholder="추가 사유"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-500 focus:ring-2 focus:ring-slate-200 disabled:bg-slate-100"
            />

            <div className="mt-1 text-right text-xs text-slate-400">
              {customReason.length}/200
            </div>
          </div>

          {customErrorMessage && (
            <div
              role="alert"
              className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {customErrorMessage}
            </div>
          )}

          {customTotalCount >= 200 && (
            <p className="mt-3 text-sm text-amber-700">
              커스텀 확장자는 최대 200개까지
              등록할 수 있습니다.
            </p>
          )}

          <div className="mt-6">
            {customPolicies.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-400">
                등록된 커스텀 확장자가 없습니다.
              </div>
            ) : (
              <div className="flex flex-wrap gap-2">
                {customPolicies.map(
                  (policy) => {
                    const isDeleting =
                      deletingIds.has(policy.id);

                    return (
                      <div
                        key={policy.id}
                        className="flex items-center gap-2 rounded-full border border-slate-300 bg-slate-50 py-1.5 pl-3 pr-2"
                      >
                        <span className="font-mono text-sm text-slate-700">
                          {policy.extension}
                        </span>

                        <button
                          type="button"
                          disabled={isDeleting}
                          onClick={() =>
                            handleDeleteCustom(
                              policy,
                            )
                          }
                          aria-label={`${policy.extension} 확장자 삭제`}
                          className="flex h-6 w-6 items-center justify-center rounded-full text-slate-400 hover:bg-slate-200 hover:text-slate-700 disabled:cursor-wait disabled:opacity-40"
                        >
                          ×
                        </button>
                      </div>
                    );
                  },
                )}
              </div>
            )}
          </div>

          {customTotalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-3">
              <button
                type="button"
                disabled={customPage === 0}
                onClick={() =>
                  loadCustomPolicies(
                    customPage - 1,
                  )
                }
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                이전
              </button>

              <span className="text-sm text-slate-500">
                {customPage + 1} /{" "}
                {customTotalPages}
              </span>

              <button
                type="button"
                disabled={
                  customPage + 1 >=
                  customTotalPages
                }
                onClick={() =>
                  loadCustomPolicies(
                    customPage + 1,
                  )
                }
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                다음
              </button>
            </div>
          )}
        </section>
        <FileUploadSection />
      </div>
    </main>
  );
}