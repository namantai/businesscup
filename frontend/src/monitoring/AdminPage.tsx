import layout from './Monitoring.module.css';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../account/AuthSession';
import { PersonalGate, PersonalError } from '../account/PersonalGate';
import { CatalogLayout } from '../catalog/CatalogLayout';
import {
  checked,
  protection,
  adminDrafts,
  adminActive,
  adminAudit,
  adminQueue,
  createCatalogDraft,
  updateCatalogDraft,
  advanceCatalogDraft,
} from '../api/account';
import type { Draft } from '../api/generated/types.gen';

export function AdminPage() {
  const { account } = useAuth();
  return (
    <CatalogLayout>
      <div className={layout.workspace}>
        <h1>Управление каталогом</h1>
        <PersonalGate>
          {account?.role === 'ADMIN' ? (
            <AdminEditor key={account.id} owner={account.id} />
          ) : (
            <p role="alert">Требуется роль ADMIN. Публикация недоступна.</p>
          )}
        </PersonalGate>
      </div>
    </CatalogLayout>
  );
}

function AdminEditor({ owner }: { owner: string }) {
  const cache = useQueryClient();
  const [selected, setSelected] = useState<Draft | null>(null);
  const [manifest, setManifest] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [message, setMessage] = useState('');
  const query = useQuery({
    queryKey: ['personal', owner, 'admin'],
    queryFn: async () => {
      const headers = { 'X-Compass-Account': owner };
      const [drafts, active, audit, queue] = await Promise.all([
        checked(adminDrafts({ headers })),
        checked(adminActive({ headers })),
        checked(adminAudit({ headers })),
        checked(adminQueue({ headers })),
      ]);
      return { drafts, active, audit, queue };
    },
  });
  async function run(action: 'SAVE' | 'VALIDATE' | 'REVIEW' | 'PUBLISH') {
    if (busy) return;
    setBusy(true);
    setError(null);
    setMessage('');
    try {
      const guard = await protection(owner);
      let result: Draft;
      if (action === 'SAVE') {
        const body = { manifest, revision: selected?.revision ?? 0 };
        result = selected
          ? await checked(
              updateCatalogDraft({ ...guard, path: { id: selected.id }, body }),
            )
          : await checked(createCatalogDraft({ ...guard, body }));
      } else {
        if (!selected || !query.data) return;
        result = await checked(
          advanceCatalogDraft({
            ...guard,
            path: { id: selected.id },
            body: {
              action,
              revision: selected.revision,
              activeRevision: query.data.active.revision,
            },
          }),
        );
      }
      setSelected(result);
      setManifest(result.manifest);
      setMessage(`Состояние: ${result.state}. Ревизия ${result.revision}.`);
      await cache.invalidateQueries({ queryKey: ['personal', owner, 'admin'] });
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <p>
        Ручной импорт: черновик → проверка → явное одобрение → публикация.
        Источники не загружаются автоматически. Правка снимает одобрение;
        опубликованный снимок неизменяем.
      </p>
      {query.isPending && <p role="status">Загружаем каталог…</p>}
      {query.error && <PersonalError error={query.error} />}
      <button disabled={busy} onClick={() => void query.refetch()}>
        Обновить
      </button>
      <p>Активная ревизия: {query.data?.active.revision ?? 'UNKNOWN'}</p>
      <h2>Черновики</h2>
      {query.data?.drafts.length === 0 && <p>Черновиков пока нет.</p>}
      <ul>
        {query.data?.drafts.map((d) => (
          <li key={d.id}>
            <button
              disabled={busy}
              onClick={() => {
                setSelected(d);
                setManifest(d.manifest);
                setMessage('');
              }}
            >
              {d.id} — {d.state}
            </button>
          </li>
        ))}
      </ul>
      <button
        disabled={busy}
        onClick={() => {
          setSelected(null);
          setManifest('');
        }}
      >
        Новый черновик
      </button>
      <fieldset disabled={busy}>
        <legend>
          {selected ? `Редактирование ${selected.id}` : 'Новый каталог'}
        </legend>
        <label>
          Manifest JSON
          <textarea
            aria-label="Manifest JSON"
            rows={12}
            value={manifest}
            onChange={(e) => setManifest(e.target.value)}
            readOnly={selected?.state === 'PUBLISHED'}
          />
        </label>
        <button
          disabled={!manifest.trim() || selected?.state === 'PUBLISHED'}
          onClick={() => void run('SAVE')}
        >
          Сохранить черновик
        </button>
        <button
          disabled={
            selected?.state !== 'DRAFT' || manifest !== selected?.manifest
          }
          onClick={() => void run('VALIDATE')}
        >
          Проверить каталог
        </button>
        <button
          disabled={
            selected?.state !== 'VALIDATED' || manifest !== selected?.manifest
          }
          onClick={() => void run('REVIEW')}
        >
          Одобрить проверенный снимок
        </button>
        <button
          disabled={
            selected?.state !== 'REVIEWED' || manifest !== selected?.manifest
          }
          onClick={() => void run('PUBLISH')}
        >
          Опубликовать
        </button>
      </fieldset>
      {busy && <p role="status">Выполняем действие…</p>}
      {message && <p role="status">{message}</p>}
      {error && <PersonalError error={error} />}
      <h2>Требуют проверки</h2>
      <p>
        STALE, CONFLICTED и UNKNOWN не становятся проверенными автоматически.
      </p>
      {query.data?.queue.length === 0 && <p>Очередь пуста.</p>}
      <ul>
        {query.data?.queue.map((e) => (
          <li key={e.id}>
            {e.summary}: {e.effectiveStatus}; проверено{' '}
            {e.verifiedAt ?? 'UNKNOWN'}. {e.conditions}
          </li>
        ))}
      </ul>
      <h2>Аудит действий</h2>
      <ul>
        {query.data?.audit.map((a) => (
          <li key={a.id}>
            {a.createdAt} — {a.action}; {a.subjectId}, ревизия {a.revision}
          </li>
        ))}
      </ul>
    </>
  );
}
