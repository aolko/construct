#include "StdAfx.h"

LuaState::LuaState()
{
	L = lua_open();
	luaL_openlibs( L );

	if( L == NULL )
	{
		std::cerr << "Lua state was NULL after creation" << std::endl;
		exit( 1 );
	}

	luabind::open( L );
	luabind::disable_super_deprecation();
	initLuaBind();
}

LuaState::~LuaState()
{
	lua_close( L );
}

lua_State* LuaState::getState()
{
	return L;
}

void LuaState::dostring( const std::string& str )
{
	printErrors( luaL_dostring( L, str.c_str() ) );
}

void LuaState::printErrors( int status )
{
	if( status )
	{
		std::cout << lua_tostring(L, -1) << std::endl;
		lua_pop(L, 1);
	}
}

void LuaState::dofile( const std::string& fname )
{
	printErrors( luaL_dofile( L, fname.c_str() ) );
}


int	LuaState::getGlobalInt(	const std::string& globalName )
{
	return luabind::object_cast<int>( luabind::globals(L)[ globalName ] );
}

float LuaState::getGlobalFloat( const std::string& globalName )
{
	return luabind::object_cast<float>( luabind::globals(L)[ globalName ] );
}

bool LuaState::getGlobalBool( const std::string& globalName )
{
	return luabind::object_cast<bool>( luabind::globals(L)[ globalName ] );
}

std::string	LuaState::getGlobalString( const std::string& globalName )
{
	return luabind::object_cast<std::string>( luabind::globals(L)[ globalName ] );
}

void LuaState::initLuaBind()
{
	using namespace luabind;
}

