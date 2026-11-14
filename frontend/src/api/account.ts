import './catalog';
import { csrf } from './generated/sdk.gen';
export * from './generated/sdk.gen';

export class AccountError extends Error {
  constructor(public status: number) {
    super('Account request failed');
  }
}
export async function checked<T>(
  request: Promise<{ data?: T; response?: Response }>,
): Promise<T> {
  const result = await request;
  if (!result.response?.ok)
    throw new AccountError(result.response?.status ?? 0);
  return result.data as T;
}
export async function protection(accountId?: string) {
  const token = await checked(csrf());
  return {
    headers: {
      [token.headerName]: token.token,
      ...(accountId ? { 'X-Compass-Account': accountId } : {}),
    },
  };
}
export function accountError(error: unknown) {
  const status = error instanceof AccountError ? error.status : 0;
  const messages: Record<number, string> = {
    400: 'Проверьте данные. Ссылка может быть недействительна или уже использована.',
    401: 'Войдите снова. Проверьте email, пароль и подтверждение адреса.',
    403: 'Действие не подтверждено. Повторите запрос; для удаления проверьте пароль.',
    404: 'Запись не найдена или недоступна вашему аккаунту.',
    409: 'Данные изменились или достигнут лимит. Обновите список: Free позволяет один профиль.',
    413: 'Данные слишком большие. Сократите профиль.',
    429: 'Слишком много попыток. Подождите 15 минут перед повтором.',
    500: 'Не удалось выполнить действие. Повторите позже.',
    503: 'Локальная почта недоступна. Проверьте режим запуска development.',
  };
  return (
    messages[status] ??
    'Нет ответа от сервера. Проверьте соединение и повторите попытку.'
  );
}
