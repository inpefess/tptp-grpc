import grpc

from tptp_proto import tptp_parser_pb2, tptp_parser_pb2_grpc

with grpc.insecure_channel("localhost:50051") as channel:
    request_proto = tptp_parser_pb2.StringMessage(
        string_message="cnf(test, axiom, $false)."
    )
    server_connection = tptp_parser_pb2_grpc.TptpParserStub(channel)
    response_proto = server_connection.parseTptp(request_proto)
    print(f"Parsed TPTP formula: {response_proto}")
