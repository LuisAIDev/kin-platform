import Link from 'next/link';

export default function Navbar() {
  return (
    <nav className="w-full bg-white border-b border-gray-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* LOGO */}
          <div className="flex-shrink-0 flex items-center">
            <span className="text-xl font-black text-gray-900 tracking-wider">KIN</span>
          </div>

          {/* ENLACES CENTRALES */}
          <div className="hidden md:flex space-x-8">
            <a href="#features" className="text-gray-600 hover:text-gray-900 font-medium text-sm transition-colors">Características</a>
            <a href="#pricing" className="text-gray-600 hover:text-gray-900 font-medium text-sm transition-colors">Precios</a>
            <a href="#contact" className="text-gray-600 hover:text-gray-900 font-medium text-sm transition-colors">Contacto</a>
          </div>

          {/* BOTONES DERECHA */}
          <div className="flex items-center space-x-4">
            <Link href="/login" className="text-gray-600 hover:text-gray-900 font-medium text-sm px-3 py-2 transition-colors">
              Iniciar sesión
            </Link>
            <Link href="/register" className="bg-gray-950 hover:bg-gray-800 text-white font-medium text-sm px-4 py-2 rounded-lg transition-all shadow-sm">
              Comenzar gratis
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}
