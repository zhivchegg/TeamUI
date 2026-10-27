import { useId, useMemo, useState, type KeyboardEvent } from 'react';

export interface PersonOption {
  id: string;
  label: string;
  /** Подсказка рядом с именем, например команда или уровень встречи. Тоже участвует в поиске. */
  hint?: string;
}

/**
 * Выбор человека с поиском: ввод фильтрует список по имени и подсказке.
 * Стрелки выбирают, Enter подтверждает, Escape закрывает список.
 */
export function PersonSelect({
  label,
  options,
  value,
  onChange,
  required = false,
  disabled = false,
  placeholder = 'Начните вводить имя',
}: {
  label: string;
  options: PersonOption[];
  value: string;
  onChange: (id: string) => void;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
}) {
  const inputId = useId();
  const listId = useId();
  const selected = options.find((o) => o.id === value) ?? null;
  const [query, setQuery] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);

  // Пока пользователь печатает, показываем его текст; иначе — имя выбранного человека.
  const text = query ?? (selected ? selected.label : '');

  const matches = useMemo(() => {
    const q = (query ?? '').trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => `${o.label} ${o.hint ?? ''}`.toLowerCase().includes(q));
  }, [options, query]);

  function choose(option: PersonOption) {
    onChange(option.id);
    setQuery(null);
    setOpen(false);
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (!open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      setOpen(true);
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((i) => Math.min(i + 1, matches.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter' && open && matches[active]) {
      e.preventDefault();
      choose(matches[active]);
    } else if (e.key === 'Escape') {
      setOpen(false);
      setQuery(null);
    }
  }

  return (
    <div className="person-select">
      <label htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        autoComplete="off"
        required={required}
        disabled={disabled}
        placeholder={placeholder}
        value={text}
        onFocus={() => {
          setOpen(true);
          setActive(0);
        }}
        // Клик по уже активному полю тоже открывает список: после выбора фокус остаётся в поле.
        onClick={() => {
          setOpen(true);
          setActive(0);
        }}
        onBlur={() => {
          setOpen(false);
          setQuery(null);
        }}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
          setActive(0);
          // Очистили поле — значит выбор сброшен.
          if (e.target.value === '') onChange('');
        }}
        onKeyDown={onKeyDown}
      />
      {open && (
        <ul id={listId} role="listbox" className="person-select-list">
          {matches.length === 0 ? (
            <li className="muted" role="option" aria-selected={false}>
              Никого не найдено
            </li>
          ) : (
            matches.map((o, i) => (
              <li
                key={o.id}
                role="option"
                aria-selected={o.id === value}
                className={i === active ? 'active' : undefined}
                // mousedown раньше blur, поэтому выбор не теряется при потере фокуса
                onMouseDown={(e) => {
                  e.preventDefault();
                  choose(o);
                }}
                onMouseEnter={() => setActive(i)}
              >
                <span className="person-name">{o.label}</span>
                {o.hint && <span className="person-hint">{o.hint}</span>}
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
