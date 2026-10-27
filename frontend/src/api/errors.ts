import { ApiError } from './client';

/**
 * Текст ошибки для пользователя. Сообщения сервера приходят на английском, поэтому для
 * известных кодов HTTP показываем русский текст, а остальное берём как есть.
 */
export function errorText(err: unknown, fallback = 'Не удалось выполнить действие.'): string {
  if (err instanceof ApiError) {
    switch (err.status) {
      case 401:
        return 'Сессия истекла. Войдите снова.';
      case 403:
        return 'Недостаточно прав для этого действия.';
      case 404:
        return 'Не найдено. Возможно, запись удалена.';
      case 409:
        return 'Данные уже изменены в другом окне. Обновите страницу и повторите.';
      default:
        return err.message || fallback;
    }
  }
  return fallback;
}
