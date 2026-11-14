import { NavLink } from 'react-router';
import styles from '../builder/Builder.module.css';
export function Navigation() {
  return (
    <nav aria-label="Основная навигация" className={styles.nav}>
      <NavLink to="/onboarding">Собрать набор</NavLink>
      <NavLink to="/catalog">Каталог</NavLink>
      <NavLink to="/results">Результаты</NavLink>
      <NavLink to="/methodology">Как это работает</NavLink>
      <NavLink to="/dashboard">Мои сохранения</NavLink>
      <NavLink to="/account">Аккаунт</NavLink>
    </nav>
  );
}
