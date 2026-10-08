import { useState } from 'react'

function MonsterDetailImage({ src, name, size = 64 }) {
  const [failedUrl, setFailedUrl] = useState(null)
  const url = src?.trim()
  return (
    <div className="detail-image" style={{ '--icon-size': `${size}px` }}>
      {url && url !== failedUrl ? (
        <img src={url} alt={name} width={size} height={size} onError={() => setFailedUrl(url)} />
      ) : (
        <span role="img" aria-label={`${name} 이미지 없음`} className="detail-image-placeholder">
          <span aria-hidden="true">◇</span>
        </span>
      )}
    </div>
  )
}

export default MonsterDetailImage
