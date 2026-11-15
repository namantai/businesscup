import { NavLink } from 'react-router';
import styles from '../builder/Builder.module.css';
export function Navigation() {
  return (
    <nav aria-label="Основная навигация" className={styles.nav}>
      <NavLink to="/" end>
        Быстрый подбор
      </NavLink>
      <NavLink to="/onboarding">Точный подбор</NavLink>
      <NavLink to="/catalog">Каталог</NavLink>
      <NavLink to="/subscriptions">Мои подписки</NavLink>
      <NavLink to="/account">Аккаунт</NavLink>
      <details className={styles.moreNav}>
        <summary>Ещё</summary>
        <div>
          <NavLink to="/results">Результаты</NavLink>
          <NavLink to="/dashboard">Мои сохранения</NavLink>
          <NavLink to="/notifications">Изменения</NavLink>
          <NavLink to="/pricing">Free / Premium</NavLink>
          <NavLink to="/methodology">Как это работает</NavLink>
        </div>
      </details>
    </nav>
  );
}
