import { useState } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Home } from './Home';
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
        <Routes>
          <Route path="/" element={<Home />} />
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
      </BrowserRouter>
    </QueryClientProvider>
  );
}
