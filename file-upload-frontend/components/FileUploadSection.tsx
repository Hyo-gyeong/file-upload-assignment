"use client";

import { useRef, useState } from "react";

import type {
  ChangeEvent,
  DragEvent,
} from "react";

import {
  FileUploadResponse,
  UploadRequestError,
  uploadFileWithProgress,
} from "@/lib/api";

type UploadStatus =
  | "READY"
  | "UPLOADING"
  | "RETRYING"
  | "SUCCESS"
  | "REJECTED"
  | "FAILED";

type UploadItem = {
  id: string;
  file: File;
  status: UploadStatus;
  progress: number;
  message?: string;
  result?: FileUploadResponse;
};

const MAX_FILE_COUNT = 10;

const MAX_TOTAL_SIZE =
  500 * 1024 * 1024;

const MAX_FILE_SIZE =
  100 * 1024 * 1024;

const MAX_AUTO_RETRIES = 3;

function formatBytes(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(
      bytes / 1024
    ).toFixed(1)} KiB`;
  }

  return `${(
    bytes /
    (1024 * 1024)
  ).toFixed(1)} MiB`;
}

function sleep(milliseconds: number) {
  return new Promise((resolve) =>
    setTimeout(resolve, milliseconds),
  );
}

function getRetryDelay(
  retryCount: number,
) {
  const randomValue =
    new Uint32Array(1);

  globalThis.crypto.getRandomValues(
    randomValue,
  );

  const jitter =
    randomValue[0] % 250;

  return (
    1000 * 2 ** retryCount +
    jitter
  );
}

function statusLabel(
  status: UploadStatus,
) {
  switch (status) {
    case "READY":
      return "준비";

    case "UPLOADING":
      return "업로드 중";

    case "RETRYING":
      return "재시도 중";

    case "SUCCESS":
      return "업로드 완료";

    case "REJECTED":
      return "거부됨";

    case "FAILED":
      return "실패";
  }
}

function statusClass(
  status: UploadStatus,
) {
  switch (status) {
    case "SUCCESS":
      return "bg-emerald-50 text-emerald-700";

    case "REJECTED":
      return "bg-amber-50 text-amber-700";

    case "FAILED":
      return "bg-red-50 text-red-700";

    case "UPLOADING":
    case "RETRYING":
      return "bg-blue-50 text-blue-700";

    default:
      return "bg-slate-100 text-slate-600";
  }
}

export default function FileUploadSection() {
  const fileInputRef =
    useRef<HTMLInputElement>(null);

  const [items, setItems] =
    useState<UploadItem[]>([]);

  const [batchError, setBatchError] =
    useState<string | null>(null);

  const [
    isBatchUploading,
    setIsBatchUploading,
  ] = useState(false);

  function updateItem(
    id: string,
    patch: Partial<UploadItem>,
  ) {
    setItems((current) =>
      current.map((item) =>
        item.id === id
          ? {
              ...item,
              ...patch,
            }
          : item,
      ),
    );
  }

  function addFiles(
    selectedFiles: File[],
  ) {
    setBatchError(null);

    const existingKeys =
      new Set(
        items.map(
          (item) =>
            `${item.file.name}:${item.file.size}:${item.file.lastModified}`,
        ),
      );

    const uniqueFiles =
      selectedFiles.filter((file) => {
        const key =
          `${file.name}:${file.size}:${file.lastModified}`;

        return !existingKeys.has(key);
      });

    if (
      items.length +
        uniqueFiles.length >
      MAX_FILE_COUNT
    ) {
      setBatchError(
        "한 번에 최대 10개의 파일만 선택할 수 있습니다.",
      );
      return;
    }

    const currentTotalSize =
      items.reduce(
        (sum, item) =>
          sum + item.file.size,
        0,
      );

    const additionalSize =
      uniqueFiles.reduce(
        (sum, file) =>
          sum + file.size,
        0,
      );

    if (
      currentTotalSize +
        additionalSize >
      MAX_TOTAL_SIZE
    ) {
      setBatchError(
        "한 번에 선택한 파일의 전체 크기는 500 MiB 이하여야 합니다.",
      );
      return;
    }

    const newItems =
      uniqueFiles.map<UploadItem>(
        (file) => {
          if (file.size === 0) {
            return {
              id: crypto.randomUUID(),
              file,
              status: "REJECTED",
              progress: 0,
              message:
                "비어 있는 파일은 업로드할 수 없습니다.",
            };
          }

          if (
            file.size >=
            MAX_FILE_SIZE
          ) {
            return {
              id: crypto.randomUUID(),
              file,
              status: "REJECTED",
              progress: 0,
              message:
                "파일 크기는 100 MiB 미만이어야 합니다.",
            };
          }

          return {
            id: crypto.randomUUID(),
            file,
            status: "READY",
            progress: 0,
          };
        },
      );

    setItems((current) => [
      ...current,
      ...newItems,
    ]);
  }

  function handleFileChange(
    event:
      ChangeEvent<HTMLInputElement>,
  ) {
    const files = Array.from(
      event.target.files ?? [],
    );

    addFiles(files);

    /*
     * 같은 파일을 다시 선택할 수 있도록
     * input 값을 초기화한다.
     */
    event.target.value = "";
  }

  function handleDrop(
    event: DragEvent<HTMLDivElement>,
  ) {
    event.preventDefault();

    if (isBatchUploading) {
      return;
    }

    addFiles(
      Array.from(
        event.dataTransfer.files,
      ),
    );
  }

  async function uploadOne(
    item: UploadItem,
  ) {
    let retryCount = 0;

    while (true) {
      updateItem(item.id, {
        status:
          retryCount === 0
            ? "UPLOADING"
            : "RETRYING",
        progress: 0,
        message:
          retryCount === 0
            ? undefined
            : `저장소 오류로 재시도 중 (${retryCount}/${MAX_AUTO_RETRIES})`,
      });

      try {
        const result =
          await uploadFileWithProgress(
            item.file,
            (progress) => {
              updateItem(item.id, {
                progress,
              });
            },
          );

        updateItem(item.id, {
          status: "SUCCESS",
          progress: 100,
          result,
          message:
            `서버에 저장되었습니다. · ${result.detectedMimeType}`,
        });

        return;
      } catch (error) {
        /*
         * 백엔드에서 R2 저장 실패가 명확하게
         * 확인된 경우에만 자동 재시도한다.
         *
         * 단순 네트워크 오류는 서버에서 이미
         * 저장을 완료했는지 알 수 없기 때문에
         * 자동 재시도하지 않는다.
         */
        const safeRetry =
          error instanceof
            UploadRequestError &&
          error.status === 502 &&
          error.code ===
            "FILE_STORAGE_FAILED";

        if (
          safeRetry &&
          retryCount <
            MAX_AUTO_RETRIES
        ) {
          const delay = getRetryDelay(retryCount);

          retryCount += 1;

          updateItem(item.id, {
            status: "RETRYING",
            message:
              `저장소 오류로 ${Math.round(
                delay / 1000,
              )}초 후 재시도합니다. ` +
              `(${retryCount}/${MAX_AUTO_RETRIES})`,
          });

          await sleep(delay);

          continue;
        }

        if (
          error instanceof
          UploadRequestError
        ) {
          const rejected =
            [
              400,
              413,
              415,
              422,
            ].includes(
              error.status ?? 0,
            );

          updateItem(item.id, {
            status: rejected
              ? "REJECTED"
              : "FAILED",
            message:
              error.code ===
              "NETWORK_ERROR"
                ? "응답 상태를 확인할 수 없어 자동 재시도하지 않았습니다. 다시 시도해주세요."
                : error.message,
          });

          return;
        }

        updateItem(item.id, {
          status: "FAILED",
          message:
            "파일 업로드 중 알 수 없는 오류가 발생했습니다.",
        });

        return;
      }
    }
  }

  async function handleUploadAll() {
    const readyItems =
      items.filter(
        (item) =>
          item.status === "READY",
      );

    if (
      readyItems.length === 0
    ) {
      setBatchError(
        "업로드할 준비 상태의 파일이 없습니다.",
      );
      return;
    }

    setBatchError(null);
    setIsBatchUploading(true);

    let cursor = 0;

    async function worker() {
      while (true) {
        const index = cursor;

        cursor += 1;

        if (
          index >=
          readyItems.length
        ) {
          return;
        }

        await uploadOne(
          readyItems[index],
        );
      }
    }

    /*
     * 동시에 최대 3개만 업로드한다.
     */
    const workerCount =
      Math.min(
        3,
        readyItems.length,
      );

    await Promise.all(
      Array.from(
        {
          length: workerCount,
        },
        () => worker(),
      ),
    );

    setIsBatchUploading(false);
  }

  async function handleRetry(
    item: UploadItem,
  ) {
    if (isBatchUploading) {
      return;
    }

    setIsBatchUploading(true);
    setBatchError(null);

    await uploadOne(item);

    setIsBatchUploading(false);
  }

  function removeItem(id: string) {
    if (isBatchUploading) {
      return;
    }

    setItems((current) =>
      current.filter(
        (item) => item.id !== id,
      ),
    );

    setBatchError(null);
  }

  function clearList() {
    if (isBatchUploading) {
      return;
    }

    setItems([]);
    setBatchError(null);
  }

  const totalSize =
    items.reduce(
      (sum, item) =>
        sum + item.file.size,
      0,
    );

  const readyCount =
    items.filter(
      (item) =>
        item.status === "READY",
    ).length;

  const successCount =
    items.filter(
      (item) =>
        item.status === "SUCCESS",
    ).length;

  const rejectedCount =
    items.filter(
      (item) =>
        item.status === "REJECTED",
    ).length;

  const failedCount =
    items.filter(
      (item) =>
        item.status === "FAILED",
    ).length;

  const processingCount =
    items.filter(
      (item) =>
        item.status === "UPLOADING" ||
        item.status === "RETRYING",
    ).length;

  const processingComplete =
    items.length > 0 &&
    readyCount === 0 &&
    processingCount === 0 &&
    !isBatchUploading;

  return (
    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">
            파일 업로드
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            서버에서 확장자 정책과 실제 파일
            형식을 다시 검증합니다.
          </p>
        </div>

        <span className="shrink-0 text-sm text-slate-500">
          {items.length} / 10
        </span>
      </div>

      <div
        onDragOver={(event) =>
          event.preventDefault()
        }
        onDrop={handleDrop}
        className="rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center"
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          hidden
          onChange={
            handleFileChange
          }
        />

        <p className="font-medium text-slate-700">
          파일을 여기로 끌어오세요
        </p>

        <p className="mt-1 text-sm text-slate-500">
          또는 파일을 직접 선택할 수
          있습니다.
        </p>

        <button
          type="button"
          disabled={isBatchUploading}
          onClick={() =>
            fileInputRef.current?.click()
          }
          className="mt-4 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
        >
          파일 선택
        </button>

        <p className="mt-4 text-xs text-slate-400">
          파일당 100 MiB 미만 · 최대 10개 ·
          전체 500 MiB 이하
        </p>
      </div>

      {batchError && (
        <div
          role="alert"
          className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
        >
          {batchError}
        </div>
      )}

      {processingComplete && (
        <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50 p-4">
          <p className="font-medium text-slate-800">
            파일 처리가 완료되었습니다.
          </p>

          <div className="mt-2 flex flex-wrap gap-x-5 gap-y-2 text-sm">
            <span className="text-emerald-700">
              업로드 완료 {successCount}개
            </span>

            <span className="text-amber-700">
              거부 {rejectedCount}개
            </span>

            {failedCount > 0 && (
              <span className="text-red-700">
                실패 {failedCount}개
              </span>
            )}
          </div>
        </div>
      )}

      {items.length > 0 && (
        <>
          <div className="mt-6 space-y-3">
            {items.map((item) => (
              <div
                key={item.id}
                className={[
                  "rounded-xl border p-4",
                  item.status === "SUCCESS"
                    ? "border-emerald-200 bg-emerald-50/20"
                    : item.status === "REJECTED"
                      ? "border-amber-200 bg-amber-50/20"
                      : item.status === "FAILED"
                        ? "border-red-200 bg-red-50/20"
                        : "border-slate-200 bg-white",
                ].join(" ")}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-slate-800">
                      {item.file.name}
                    </p>

                    <p className="mt-1 text-xs text-slate-400">
                      {formatBytes(
                        item.file.size,
                      )}
                    </p>
                  </div>

                  <span
                    className={[
                      "shrink-0 rounded-full px-2.5 py-1 text-xs font-medium",
                      statusClass(
                        item.status,
                      ),
                    ].join(" ")}
                  >
                    {statusLabel(
                      item.status,
                    )}
                  </span>
                </div>

                {[
                  "UPLOADING",
                  "RETRYING",
                  "SUCCESS",
                ].includes(
                  item.status,
                ) && (
                  <div className="mt-3">
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full bg-slate-800 transition-all"
                        style={{
                          width:
                            `${item.progress}%`,
                        }}
                      />
                    </div>

                    <div className="mt-1 text-right text-xs text-slate-400">
                      {item.progress}%
                    </div>
                  </div>
                )}

                {item.message && (
                  <div className="mt-3">
                    {item.status ===
                      "SUCCESS" && (
                      <p className="text-sm font-medium text-emerald-700">
                        업로드가 완료되었습니다.
                      </p>
                    )}

                    {item.status ===
                      "REJECTED" && (
                      <p className="text-sm font-medium text-amber-700">
                        서버에 저장되지 않았습니다.
                      </p>
                    )}

                    {item.status ===
                      "FAILED" && (
                      <p className="text-sm font-medium text-red-700">
                        업로드를 완료하지 못했습니다.
                      </p>
                    )}

                    <p
                      className={[
                        "mt-1 text-sm",
                        item.status ===
                          "SUCCESS"
                          ? "text-slate-500"
                          : item.status ===
                              "REJECTED"
                            ? "text-amber-700"
                            : item.status ===
                                "FAILED"
                              ? "text-red-600"
                              : "text-slate-500",
                      ].join(" ")}
                    >
                      {item.message}
                    </p>
                  </div>
                )}

                {[
                  "SUCCESS",
                  "REJECTED",
                  "FAILED",
                ].includes(
                  item.status,
                ) && (
                  <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-slate-100 pt-3">
                    {item.status ===
                      "FAILED" && (
                      <button
                        type="button"
                        disabled={
                          isBatchUploading
                        }
                        onClick={() =>
                          handleRetry(
                            item,
                          )
                        }
                        className="text-sm font-medium text-blue-600 hover:underline disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        다시 시도
                      </button>
                    )}

                    <button
                      type="button"
                      disabled={
                        isBatchUploading
                      }
                      onClick={() =>
                        removeItem(
                          item.id,
                        )
                      }
                      className="text-sm text-slate-500 hover:text-slate-800 hover:underline disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      {item.status ===
                      "SUCCESS"
                        ? "목록에서 숨기기"
                        : "목록에서 제거"}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="mt-5 flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500">
                총{" "}
                {formatBytes(
                  totalSize,
                )}
                {readyCount > 0 && (
                  <>
                    {" · "}업로드 대기{" "}
                    {readyCount}개
                  </>
                )}
              </p>

              {processingComplete && (
                <p className="mt-1 text-xs text-slate-400">
                  목록을 비워도 이미 업로드된
                  서버 파일은 삭제되지 않습니다.
                </p>
              )}
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                disabled={
                  isBatchUploading
                }
                onClick={clearList}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                {processingComplete
                  ? "결과 목록 비우기"
                  : "목록 비우기"}
              </button>

              {readyCount > 0 && (
                <button
                  type="button"
                  disabled={
                    isBatchUploading
                  }
                  onClick={
                    handleUploadAll
                  }
                  className="rounded-lg bg-slate-900 px-5 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                >
                  {isBatchUploading
                    ? "업로드 중..."
                    : `업로드 (${readyCount})`}
                </button>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  );
}