import { createBrowserRouter, RouterProvider, Outlet } from 'react-router-dom';
import { Layout } from '../shared/components/Layout';
import Dashboard from '../features/dashboard/components/Dashboard';
import ThreatIntel from '../features/threat/components/ThreatIntel';
import LogsLedger from '../features/logs/components/LogsLedger';
import PipelineFlow from '../features/pipeline/components/PipelineFlow';
import ServiceRegistry from '../features/registry/components/ServiceRegistry';
import Infrastructure from '../features/infra/components/Infrastructure';

const router = createBrowserRouter([
  {
    element: <Layout><Outlet /></Layout>,
    children: [
      { path: '/', element: <Dashboard /> },
      { path: '/threat', element: <ThreatIntel /> },
      { path: '/logs', element: <LogsLedger /> },
      { path: '/pipeline', element: <PipelineFlow /> },
      { path: '/registry', element: <ServiceRegistry /> },
      { path: '/infra', element: <Infrastructure /> },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
