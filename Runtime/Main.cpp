// D3D 2D draw.cpp : Defines the entry point for the application.
//

/*
	Construct - DirectX 9 Game and Application creation
    Copyright (C) 2007 Scirra
*/

// Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
//   in the documentation and/or other materials provided with the distribution.
// * Neither the name of the Scirra nor the names of its contributors may be used to endorse or promote products derived
//   from this software without specific prior written permission.
//
//	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
//  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
//  SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
//  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
//  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#include "stdafx.h"

#ifdef CONSTRUCT_DEBUGGER
#include <sstream>
CString DebuggerGetError(const CRuntime& rt, CRunObject* o, CString aceType);
#endif

////////////////////////////////////////////////
int APIENTRY WinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPSTR     lpCmdLine,
                     int       nCmdShow)
{
	// Set hInstance
	CRuntimeSetup crSetup;
	crSetup.hInstance = hInstance;

	InitCommonControls();
	INITCOMMONCONTROLSEX lp;
	lp.dwICC = ICC_USEREX_CLASSES | ICC_WIN95_CLASSES | ICC_TAB_CLASSES | ICC_STANDARD_CLASSES | ICC_BAR_CLASSES;
	lp.dwSize = sizeof(lp);
	InitCommonControlsEx(&lp);

#ifdef CONSTRUCT_DIRECTX9
	// Set DirectX resolution format (always X8R8G8B8)
	crSetup.display_params.backbuffer_format = cr::texture_format_x8r8g8b8;
	crSetup.display_params.swap = cr::swap_discard;
#endif
 
	///////////////////////////////////
	// CONSTRUCT AND INITIALISE RUNTIME
	bool success = true;

	CRuntime Runtime(success);

	// Initialisation failed
	if (!success) return 0;

	try {

#ifdef CONSTRUCT_DEBUGGER

		AfxSetResourceHandle(hInstance);
		AfxGetModuleState()->m_hCurrentInstanceHandle = hInstance;

		CDebugger Debugger;

		Debugger.pRuntime = &Runtime;
		Runtime.pDebug = &Debugger;

		CObjectsPage	ObjectPage;
		CWatchPage		WatchPage;
		CLogPage		LogPage;

		ObjectPage.pWatchPage = &WatchPage;
		ObjectPage.pRuntime = &Runtime;
		Runtime.pObjectPage = &ObjectPage;

		WatchPage.pObjectsPage = &ObjectPage;
		WatchPage.pRuntime = &Runtime;
		Runtime.pWatchPage = &WatchPage;

		LogPage.pRuntime = &Runtime;
		Runtime.pLogPage = &LogPage;

		Debugger.AddPage(&ObjectPage);
		Debugger.AddPage(&WatchPage);
		Debugger.AddPage(&LogPage);
#endif
		Runtime.Initialise(&crSetup);

		//////////////////
		// RUN APPLICATION
		Runtime.Run();

	}
	
	
	/////////////////////////////////
	// RUNTIME THROWN ERROR MESSAGES
	catch (runtime_error e) {

		// Completely close down runtime
		Runtime.Close();

		// Show message before ending
		MessageBox(NULL, e.what(), "Runtime error", MB_OK | MB_ICONHAND);

	}
	

	/////////////////////////////////
	// DISPLAY THROWN ERROR MESSAGES

#ifdef _DEBUG
#define DEBUG_NO_CATCHING_CRASHES
#endif

#if !defined(APPRUNTIME) && !defined(DEBUG_NO_CATCHING_CRASHES)
	catch (cr::CDX9Error d) {
		
		Runtime.Close();

		CString s;
		s.Format("Construct encountered an error in the display engine.  Please report this problem to the application developer.\n\n"
			"%s (0x%x)", d.GetMsg(), d.GetCode());

		MessageBox(NULL, s, "Runtime error", MB_OK | MB_ICONHAND);

		return 0;

	}
#endif

#ifndef DEBUG_NO_CATCHING_CRASHES

	///////////////
	// HARD CRASH
	// A plugin may have messed up, or it could be a bug in the runtime.
	catch (...) {

		///////////////////////////////
		// DEBUGGER RUNTIME HARD CRASH
		// Attempt to record details about the crash.  If that fails, send standard message.
#ifdef CONSTRUCT_DEBUGGER

		CString msg;
		CRunObject* o = Runtime.curObject;

		// Find the area of the runtime the error was thrown from
		try {
			switch (Runtime.dcs) {
				case DCS_ACTION:
					msg = DebuggerGetError(Runtime, o, "action");
					break;

				case DCS_CONDITION:
					msg = DebuggerGetError(Runtime, o, "condition");
					break;

				case DCS_EXPRESSION:
					msg = DebuggerGetError(Runtime, o, "expression");
					break;

				case DCS_PLUGIN:
					msg = DebuggerGetError(Runtime, o, "plugin");
					break;

				case DCS_RUNTIME:
					msg = "A crash inside the runtime has been intercepted!  The crash may be a bug in Construct.  Please report this to Scirra.";
					break;

				default:
					msg = "Universal Logic Fault!  Oh no!";
			}
		}
		catch (...) {
			// Error occurred trying to form the error message!  Perhaps a bad 'o' pointer
			msg = "A crash was intercepted, but information could not be obtained about the runtime state.  This may be a bug in Construct, please report this error message to Scirra.";
		}

		// Close runtime and display message
		Runtime.Close();

		MessageBox(NULL, msg, "Runtime error", MB_OK | MB_ICONHAND);

#else //CONSTRUCT_DEBUGGER

		Runtime.Close();

		// In preview mode, we can inform the user that the Construct runtime has messed up.
#ifdef CONSTRUCT_PREVIEW
		MessageBox(NULL, "An unexpected error occurred and the application was terminated.\n\nYou may be able to find out more information about the crash by Debugging, and attempting to reproduce the bug.", "Runtime error", MB_OK | MB_ICONHAND);
#else
		// In exported applications, give the user a polite message, since this is what will be seen by our user's end-users when their apps mess up.
		MessageBox(NULL, "An unexpected error occurred and the application was terminated.  We apologise for the inconvenience.\n\nPlease report this problem to the application developer.", "Runtime error", MB_OK | MB_ICONHAND);
#endif

#endif //NOT CONSTRUCT_DEBUGGER
		return 0;
	}

#endif //DEBUG_NO_CATCHING_CRASHES

	return 0;
}

#ifdef CONSTRUCT_DEBUGGER

// Debugger error
CString DebuggerGetError(const CRuntime& rt, CRunObject* o, CString aceType)
{
	stringstream msg;

	bool system = (o == &rt.system);
	
	if (system)
		msg << "A crash inside a system object ";
	else
		msg << "A crash inside a plugin's ";

	if (aceType == "plugin")
		msg << "non-event code";
	else
		msg << aceType;

	msg << " has been intercepted!  ";
	
	if (system)
		msg << "You should report this problem to Scirra.  ";
	else
		msg << "This may be a bug in the plugin, or a problem in Construct.  ";

	msg << "The application has exited.  Available details of the location of the problem are below (this may be approximate).\n\n";

	msg << "Plugin: ";

	if (system)
		msg << "<System object>";
	else
		msg << o->pType->pPlugin->previewName;

	msg << "\n";

	if (!system)
		msg << "Object name: " << o->pType->Name << "\n";

	if (rt.pCurrentEvent != NULL && aceType != "plugin")
		msg << "Event " << rt.pCurrentEvent->number << " in event sheet '" << rt.eventSheetNames[rt.pCurrentEvent->eventSheet] << "'\n";

	if (aceType == "action")
		msg << "Action: " << g_actionIndex << "\n";
	else if (aceType == "condition")
		msg << "Condition: " << (*(rt.pCurrentEvent->pCurCnd))->number << "\n";
	else if (aceType == "expression") {
		if (g_actionIndex > 0)
			msg << "Inside action: " << g_actionIndex << "\n";
		else
			msg << "Inside condition: " << (*(rt.pCurrentEvent->pCurCnd))->number << "\n";
	}

	if (!system)
		msg << "Instance: " << (o->info.iid + 1) << " (of " << (o->pType->instances.size()) << ")";

	return msg.str().c_str();
}

#endif