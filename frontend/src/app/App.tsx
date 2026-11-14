import { useState } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BuilderSession } from '../builder/BuilderSession';
import { BuilderPage } from '../builder/BuilderPage';
import { ResultsPage } from '../builder/ResultsPage';
import { Methodology } from './Methodology';
import { Home } from './Home';
import { AuthSession } from '../account/AuthSession';
import { AuthPage } from '../account/AuthPage';
import { Dashboard } from '../account/Dashboard';
import { AccountPage } from '../account/AccountPage';
import { CatalogList } from '../catalog/CatalogList';
import { ProductDetail } from '../catalog/ProductDetail';

export function App() {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: false, refetchOnWindowFocus: false },
        },
      }),
  );
  return (
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <BuilderSession>
          <AuthSession>
            <Routes>
              <Route
                path="/register"
                element={<AuthPage key="register" mode="register" />}
              />
              <Route
                path="/login"
                element={<AuthPage key="login" mode="login" />}
              />
              <Route
                path="/verify-email"
                element={<AuthPage key="verify" mode="verify" />}
              />
              <Route
                path="/forgot-password"
                element={<AuthPage key="forgot" mode="forgot" />}
              />
              <Route
                path="/reset-password"
                element={<AuthPage key="reset" mode="reset" />}
              />
              <Route path="/account" element={<AccountPage />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/" element={<Home />} />
              <Route path="/onboarding" element={<BuilderPage />} />
              <Route path="/results" element={<ResultsPage />} />
              <Route path="/methodology" element={<Methodology />} />
              <Route path="/catalog" element={<CatalogList />} />
              <Route path="/catalog/:slug" element={<ProductDetail />} />
              <Route
                path="*"
                element={
                  <main>
                    <h1>Страница не найдена</h1>
                    <a href="/catalog">Открыть каталог</a>
                  </main>
                }
              />
            </Routes>
          </AuthSession>
        </BuilderSession>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
