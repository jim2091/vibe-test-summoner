import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout.jsx'
import MonsterListPage from './pages/MonsterListPage.jsx'
import MonsterDetailPage from './pages/MonsterDetailPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/monsters" replace />} />
          <Route path="/monsters" element={<MonsterListPage />} />
          <Route path="/monsters/:monsterId" element={<MonsterDetailPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
