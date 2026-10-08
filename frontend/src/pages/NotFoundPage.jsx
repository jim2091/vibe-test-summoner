import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <>
      <h1>404</h1>
      <p>요청하신 페이지를 찾을 수 없습니다.</p>
      <Link to="/monsters">몬스터 목록으로 돌아가기</Link>
    </>
  )
}

export default NotFoundPage
