import { Providers } from './providers';
import { AppRouter } from './routes';

export function App() {
  return (
    <Providers>
      <AppRouter />
    </Providers>
  );
}
