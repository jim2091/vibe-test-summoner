import { Link, Outlet } from 'react-router-dom'

function MainLayout() {
  return (
    <>
      <header className="site-header">
        <div className="content-width">
          <Link className="site-name" to="/monsters">Summoners Archive</Link>
        </div>
      </header>
      <main className="content-width main-content">
        <Outlet />
      </main>
    </>
  )
}

export default MainLayout
