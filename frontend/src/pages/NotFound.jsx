import { Link } from 'react-router-dom'
export default function NotFound() { return <div className="not-found"><span>404</span><h1>Page not found</h1><p>This page doesn't exist or may have moved.</p><Link to="/" className="primary-button">Go home</Link></div> }
