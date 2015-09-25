// affinityInfo.cpp : Definiert den Einstiegspunkt für die Konsolenanwendung.
//

#include "stdafx.h"
#include <Windows.h>
#include <iostream>
#include <iomanip>
#include <bitset>
#include <vector>

using namespace std;

void printBitSet(bitset<64> bs) {
	for (size_t i = bs.size() - 1; i >= 0; i--) {
		cout << (bs.test(i) ? "1" : "0");
		if ((i > 0) && (i % 8 == 0)) {
			cout << ".";
		}
		if (i == 0) {
			return;
		}
	}
}

void printMask(GROUP_AFFINITY &mask) {
	bitset<64> bs(mask.Mask);
	cout << "g_" << mask.Group << "/";
	printBitSet(bs);
}

void printCacheRelation(CACHE_RELATIONSHIP &cr) {
	cout << " cache L" << (int) cr.Level;
	if (cr.Associativity == 0xFF) {
		cout << "fully assoc";
	}
	else {
		cout << " " << setw(2) << (int)cr.Associativity << " x assoc";
	}
	char *unit = " B";
	long cs = cr.CacheSize;
	if (cs % 1024 == 0) {
		cs /= 1024;
		unit = " K";
	}
	if (cs % 1024 == 0) {
		cs /= 1024;
		unit = " M";
	}
	cout << " Size " << setw(3) << cs << unit;
	cout << " Type ";
	switch (cr.Type) {
	case CacheData:
		cout << "D"; break;
	case CacheInstruction:
		cout << "I"; break;
	case CacheUnified:
		cout << "U"; break;
	case CacheTrace:
		cout << "T"; break;
	}
	cout << " mask: ";
	printMask( cr.GroupMask);
}

void printGroupInfo(PROCESSOR_GROUP_INFO &pgi) {
	int apc = pgi.ActiveProcessorCount;
	int mpc = pgi.MaximumProcessorCount;
	cout << " procs active " << setw(3) << apc << " of " << setw(3) << mpc;
	cout << " ";
	bitset<64> bs(pgi.ActiveProcessorMask);
	printBitSet(bs);
}

void printGroupRelation(GROUP_RELATIONSHIP &gr) {
	cout << " groups active: " << gr.ActiveGroupCount << " of " << gr.MaximumGroupCount;
	if (gr.ActiveGroupCount == 1) {
		printGroupInfo( gr.GroupInfo[0]);
	}
	else {
		cout << "\n";
		PROCESSOR_GROUP_INFO *pgi = gr.GroupInfo;
		for (int i = 0; i < gr.ActiveGroupCount; i++) {
			cout << "group " << i << ": ";
			printGroupInfo( *pgi++);
			cout << "\n";
		}
	}
}

void printNodeRelation(NUMA_NODE_RELATIONSHIP &nr) {
	cout << " node N#" << nr.NodeNumber;
	printMask( nr.GroupMask);
}

void printProcessorRelation(PROCESSOR_RELATIONSHIP &pr) {
	cout << " core SMT " << (pr.Flags == 0 ? "Off" : "On");
	cout << " ";
	if (pr.GroupCount == 1) {
		printMask( pr.GroupMask[0]);
	}
	else {
		for (int i = 0; i < pr.GroupCount; i++) {
			printMask( pr.GroupMask[i]);
		}
	}
}

void printSystemRelationShipInfo(SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX &info) {

	switch (info.Relationship) {
	case RelationCache:				cout << "Ca"; break;
	case RelationGroup:				cout << "Gr"; break;
	case RelationNumaNode:			cout << "Nu"; break;
	case RelationProcessorCore:		cout << "Co"; break;
	case RelationProcessorPackage:	cout << "So"; break;
	default:
		cout << "?";
		break;
	}
	cout << " size " << setw(3) << info.Size << " ";
	switch (info.Relationship) {
	case RelationCache:				printCacheRelation( info.Cache); break;
	case RelationGroup:				printGroupRelation( info.Group); break;
	case RelationNumaNode:			printNodeRelation( info.NumaNode); break;
	case RelationProcessorCore:
	case RelationProcessorPackage:	printProcessorRelation( info.Processor); break;
	default:
		cout << "?";
		break;
	}
}

SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *getSystemRelationShipInfos(int& num) {

	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *retPtr;
	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX	dummy;
	retPtr = &dummy;
	DWORD	bufSize = sizeof(dummy);

	do {
		if (GetLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP::RelationAll, retPtr, &bufSize)) {
			num = bufSize;
			return retPtr;
		}
		if (retPtr != &dummy) {
			free(retPtr);
		}
		retPtr = (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *)malloc(bufSize);
	} while (GetLastError() == 122);

	if (retPtr != &dummy) {
		free(retPtr);
	}
	num = 0;
	return NULL;
}

#ifdef __cplusplus
extern "C" {
#endif
__declspec(dllexport)
BOOL getSystemRelationShipInfos(PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX** retList, int &count) {
	int	bufSize;
	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *retVal = getSystemRelationShipInfos(bufSize);

	count = 0;
	if (retVal == NULL) {
		*retList = NULL;
		return false;
	}

	// first loop: count entries only
	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *info = retVal;
	byte *ptr = (byte*)info;
	byte *ptrStart = ptr;
	while (ptr < (ptrStart + bufSize)) {
		count++;
		ptr += info->Size;
		info = (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX*)ptr;
	}
	// allocate array and put entries in it
	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX**	result = (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX**)calloc(count, sizeof(SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX *));
	info = retVal;
	ptr = (byte*)info;
	ptrStart = ptr;
	int i = 0;
	while (ptr < (ptrStart + bufSize)) {
		// cout << "at: " << hex << info << ": " << dec << "info #" << setw(3) << dec << i;
		// printSystemRelationShipInfo(*info);
		// cout << "\n";
		// cout.flush();
		result[i++] = info;
		ptr += info->Size;
		info = (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX*)ptr;
	}

	*retList = result;
	// cout << "putting " << hex << result << " at " << hex << retList;
	return true;
}
#ifdef __cplusplus
}
#endif


int _tmain(int argc, _TCHAR* argv[])
{	
	int	count;
	SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX**	list;
	BOOL success = getSystemRelationShipInfos( &list, count);
	if (!success) {
		return 1;
	}

	int i = 0;
	for (int rel = RelationProcessorCore; rel <= RelationGroup; rel++) {
		for (i = 0; i < count;  i++) {
			auto inf = list[i];
			if (inf->Relationship == rel) {
				cout << "info " << setw(3) << i++ << ": ";
				printSystemRelationShipInfo( *inf);
				cout << "\n";
			}
		}
	}
	return 0;
}

